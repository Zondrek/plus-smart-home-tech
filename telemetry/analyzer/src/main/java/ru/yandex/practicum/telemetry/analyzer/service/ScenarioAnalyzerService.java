package ru.yandex.practicum.telemetry.analyzer.service;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc.HubRouterControllerBlockingStub;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.model.*;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;

import java.util.List;
import java.util.Map;

/**
 * Сервис для анализа сценариев и выполнения действий.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioAnalyzerService {

    @GrpcClient("hub-router")
    private HubRouterControllerBlockingStub hubRouterClient;

    private final ScenarioRepository scenarioRepository;

    /**
     * Анализирует снапшот и выполняет подходящие сценарии.
     */
    @Transactional(readOnly = true)
    public void analyzeSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        log.debug("Analyzing snapshot for hub: {}", hubId);

        // Загружаем все сценарии для данного хаба
        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);

        if (scenarios.isEmpty()) {
            log.debug("No scenarios found for hub: {}", hubId);
            return;
        }

        // Получаем карту состояний датчиков (ключ - id датчика, значение - состояние)
        Map<String, SensorStateAvro> sensorStates = snapshot.getSensorsState();

        // Проверяем каждый сценарий
        for (Scenario scenario : scenarios) {
            try {
                if (checkScenarioConditions(scenario, sensorStates)) {
                    log.info("Scenario {} triggered for hub {}", scenario.getName(), hubId);
                    executeScenarioActions(scenario, snapshot);
                }
            } catch (Exception e) {
                log.error("Error processing scenario {} for hub {}", scenario.getName(), hubId, e);
            }
        }
    }

    /**
     * Проверяет, выполняются ли все условия сценария.
     */
    private boolean checkScenarioConditions(Scenario scenario, Map<String, SensorStateAvro> sensorStates) {
        for (ScenarioCondition scenarioCondition : scenario.getConditions()) {
            Condition condition = scenarioCondition.getCondition();
            String sensorId = scenarioCondition.getSensor().getId();

            SensorStateAvro state = sensorStates.get(sensorId);
            if (state == null) {
                log.debug("Sensor {} not found in snapshot", sensorId);
                return false;
            }

            if (!checkCondition(condition, state)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Проверяет одно условие на соответствие состоянию датчика.
     */
    private boolean checkCondition(Condition condition, SensorStateAvro state) {
        Object sensorValue = extractSensorValue(condition.getType(), state);

        if (sensorValue == null) {
            return false;
        }

        Integer expectedValue = condition.getValue();
        ConditionOperation operation = condition.getOperation();

        if (sensorValue instanceof Integer intValue) {
            return switch (operation) {
                case EQUALS -> intValue.equals(expectedValue);
                case GREATER_THAN -> intValue > expectedValue;
                case LOWER_THAN -> intValue < expectedValue;
            };
        } else if (sensorValue instanceof Boolean boolValue) {
            boolean expected = expectedValue == 1;
            return switch (operation) {
                case EQUALS -> boolValue == expected;
                default -> false;
            };
        }

        return false;
    }

    /**
     * Извлекает значение датчика в зависимости от типа условия.
     */
    private Object extractSensorValue(ConditionType type, SensorStateAvro state) {
        Object data = state.getData();

        return switch (type) {
            case MOTION -> data instanceof MotionSensorAvro motion ? motion.getMotion() : null;
            case LUMINOSITY -> data instanceof LightSensorAvro light ? light.getLuminosity() : null;
            case SWITCH -> data instanceof SwitchSensorAvro switchSensor ? switchSensor.getState() : null;
            case TEMPERATURE -> {
                if (data instanceof TemperatureSensorAvro temp) {
                    yield temp.getTemperatureC();
                } else if (data instanceof ClimateSensorAvro climate) {
                    yield climate.getTemperatureC();
                }
                yield null;
            }
            case CO2LEVEL -> data instanceof ClimateSensorAvro climate ? climate.getCo2Level() : null;
            case HUMIDITY -> data instanceof ClimateSensorAvro climate ? climate.getHumidity() : null;
        };
    }

    /**
     * Выполняет действия сценария.
     */
    private void executeScenarioActions(Scenario scenario, SensorsSnapshotAvro snapshot) {
        for (ScenarioAction scenarioAction : scenario.getActions()) {
            Action action = scenarioAction.getAction();
            String sensorId = scenarioAction.getSensor().getId();

            try {
                DeviceActionProto actionProto = DeviceActionProto.newBuilder()
                        .setSensorId(sensorId)
                        .setType(ActionTypeProto.valueOf(action.getType().name()))
                        .setValue(action.getValue() != null ? action.getValue() : 0)
                        .build();

                DeviceActionRequest request = DeviceActionRequest.newBuilder()
                        .setHubId(snapshot.getHubId())
                        .setScenarioName(scenario.getName())
                        .setAction(actionProto)
                        .setTimestamp(Timestamp.newBuilder()
                                .setSeconds(snapshot.getTimestamp().getEpochSecond())
                                .setNanos(snapshot.getTimestamp().getNano())
                                .build())
                        .build();

                var response = hubRouterClient.handleDeviceAction(request);
                log.info("Executed action {} on sensor {} for scenario {}: {}",
                        action.getType(), sensorId, scenario.getName(), response);
            } catch (Exception e) {
                log.error("Error executing action for scenario {}", scenario.getName(), e);
            }
        }
    }
}