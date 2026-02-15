package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.model.*;
import ru.yandex.practicum.telemetry.analyzer.repository.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис для обработки событий от хабов.
 * Обрабатывает добавление/удаление устройств и сценариев.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HubEventService {

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    /**
     * Обрабатывает событие от хаба.
     */
    @Transactional
    public void handleHubEvent(HubEventAvro event) {
        String hubId = event.getHubId();

        if (event.getPayload() instanceof DeviceAddedEventAvro deviceAdded) {
            handleDeviceAdded(hubId, deviceAdded);
        } else if (event.getPayload() instanceof DeviceRemovedEventAvro deviceRemoved) {
            handleDeviceRemoved(hubId, deviceRemoved);
        } else if (event.getPayload() instanceof ScenarioAddedEventAvro scenarioAdded) {
            handleScenarioAdded(hubId, scenarioAdded);
        } else if (event.getPayload() instanceof ScenarioRemovedEventAvro scenarioRemoved) {
            handleScenarioRemoved(hubId, scenarioRemoved);
        }
    }

    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro event) {
        String sensorId = event.getId();

        if (sensorRepository.existsById(sensorId)) {
            log.debug("Sensor {} already exists, skipping", sensorId);
            return;
        }

        Sensor sensor = new Sensor(sensorId, hubId);
        sensorRepository.save(sensor);
        log.info("Added sensor: {} for hub: {}", sensorId, hubId);
    }

    private void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro event) {
        String sensorId = event.getId();

        sensorRepository.findByIdAndHubId(sensorId, hubId).ifPresent(sensor -> {
            sensorRepository.delete(sensor);
            log.info("Removed sensor: {} from hub: {}", sensorId, hubId);
        });
    }

    private void handleScenarioAdded(String hubId, ScenarioAddedEventAvro event) {
        String scenarioName = event.getName();

        // Проверяем, существует ли сценарий
        Scenario scenario = scenarioRepository.findByHubIdAndName(hubId, scenarioName)
                .orElse(null);

        // Загружаем и валидируем датчики
        Map<String, Sensor> sensorsMap = loadAndValidateSensors(hubId, scenarioName, event);

        if (scenario != null) {
            // Сценарий существует - проверяем, изменились ли условия или действия
            if (hasScenarioChanged(scenario, event, sensorsMap)) {
                log.info("Scenario {} for hub {} has changed, updating", scenarioName, hubId);

                // Очищаем старые условия и действия
                scenario.getConditions().clear();
                scenario.getActions().clear();

                // Обновляем условия и действия
                processConditions(scenario, event.getConditions(), sensorsMap);
                processActions(scenario, event.getActions(), sensorsMap);

                scenarioRepository.save(scenario);
                log.info("Updated scenario: {} for hub: {}", scenarioName, hubId);
            } else {
                log.debug("Scenario {} for hub {} unchanged, skipping", scenarioName, hubId);
            }
        } else {
            // Создаем новый сценарий
            scenario = new Scenario();
            scenario.setHubId(hubId);
            scenario.setName(scenarioName);
            scenario.setConditions(new ArrayList<>());
            scenario.setActions(new ArrayList<>());

            // Сохраняем сценарий, чтобы получить ID для composite keys
            scenario = scenarioRepository.save(scenario);

            // Обрабатываем условия и действия
            processConditions(scenario, event.getConditions(), sensorsMap);
            processActions(scenario, event.getActions(), sensorsMap);

            scenarioRepository.save(scenario);
            log.info("Added scenario: {} for hub: {}", scenarioName, hubId);
        }
    }

    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro event) {
        String scenarioName = event.getName();

        scenarioRepository.findByHubIdAndName(hubId, scenarioName).ifPresent(scenario -> {
            scenarioRepository.delete(scenario);
            log.info("Removed scenario: {} from hub: {}", scenarioName, hubId);
        });
    }

    private void processConditions(Scenario scenario, List<ScenarioConditionAvro> conditionAvros, Map<String, Sensor> sensorsMap) {
        if (conditionAvros.isEmpty()) {
            return;
        }

        // Подготавливаем условия для сохранения
        List<Condition> conditions = new ArrayList<>();
        for (ScenarioConditionAvro conditionAvro : conditionAvros) {
            Condition condition = new Condition();
            condition.setType(ConditionType.valueOf(conditionAvro.getType().name()));
            condition.setOperation(ConditionOperation.valueOf(conditionAvro.getOperation().name()));

            // Обрабатываем значение условия
            Object value = conditionAvro.getValue();
            if (value instanceof Integer intValue) {
                condition.setValue(intValue);
            } else if (value instanceof Boolean boolValue) {
                condition.setValue(boolValue ? 1 : 0);
            }

            conditions.add(condition);
        }

        // Сохраняем все условия одним запросом
        List<Condition> savedConditions = conditionRepository.saveAll(conditions);

        // Создаем связи между сценарием и условиями
        for (int i = 0; i < conditionAvros.size(); i++) {
            ScenarioConditionAvro conditionAvro = conditionAvros.get(i);
            Condition savedCondition = savedConditions.get(i);

            ScenarioConditionId id = new ScenarioConditionId(
                    scenario.getId(),
                    conditionAvro.getSensorId(),
                    savedCondition.getId()
            );

            Sensor sensor = sensorsMap.get(conditionAvro.getSensorId());

            ScenarioCondition scenarioCondition = new ScenarioCondition();
            scenarioCondition.setId(id);
            scenarioCondition.setScenario(scenario);
            scenarioCondition.setSensor(sensor);
            scenarioCondition.setCondition(savedCondition);

            scenario.getConditions().add(scenarioCondition);
        }
    }

    private void processActions(Scenario scenario, List<DeviceActionAvro> actionAvros, Map<String, Sensor> sensorsMap) {
        if (actionAvros.isEmpty()) {
            return;
        }

        // Подготавливаем действия для сохранения
        List<Action> actions = new ArrayList<>();
        for (DeviceActionAvro actionAvro : actionAvros) {
            Action action = new Action();
            action.setType(ActionType.valueOf(actionAvro.getType().name()));
            action.setValue(actionAvro.getValue());
            actions.add(action);
        }

        // Сохраняем все действия одним запросом
        List<Action> savedActions = actionRepository.saveAll(actions);

        // Создаем связи между сценарием и действиями
        for (int i = 0; i < actionAvros.size(); i++) {
            DeviceActionAvro actionAvro = actionAvros.get(i);
            Action savedAction = savedActions.get(i);

            ScenarioActionId id = new ScenarioActionId(
                    scenario.getId(),
                    actionAvro.getSensorId(),
                    savedAction.getId()
            );

            Sensor sensor = sensorsMap.get(actionAvro.getSensorId());

            ScenarioAction scenarioAction = new ScenarioAction();
            scenarioAction.setId(id);
            scenarioAction.setScenario(scenario);
            scenarioAction.setSensor(sensor);
            scenarioAction.setAction(savedAction);

            scenario.getActions().add(scenarioAction);
        }
    }

    private boolean hasScenarioChanged(Scenario scenario, ScenarioAddedEventAvro event, Map<String, Sensor> sensorsMap) {
        // Проверяем количество условий и действий
        if (scenario.getConditions().size() != event.getConditions().size() ||
                scenario.getActions().size() != event.getActions().size()) {
            return true;
        }

        // Проверяем условия
        for (int i = 0; i < event.getConditions().size(); i++) {
            ScenarioConditionAvro newCondition = event.getConditions().get(i);

            // Ищем соответствующее существующее условие
            boolean found = scenario.getConditions().stream()
                    .anyMatch(sc -> matchesCondition(sc, newCondition));

            if (!found) {
                return true;
            }
        }

        // Проверяем действия
        for (int i = 0; i < event.getActions().size(); i++) {
            DeviceActionAvro newAction = event.getActions().get(i);

            // Ищем соответствующее существующее действие
            boolean found = scenario.getActions().stream()
                    .anyMatch(sa -> matchesAction(sa, newAction));

            if (!found) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesCondition(ScenarioCondition scenarioCondition, ScenarioConditionAvro conditionAvro) {
        Condition condition = scenarioCondition.getCondition();

        // Сравниваем тип, операцию и значение
        boolean typeMatches = condition.getType().name().equals(conditionAvro.getType().name());
        boolean operationMatches = condition.getOperation().name().equals(conditionAvro.getOperation().name());
        boolean sensorMatches = scenarioCondition.getSensor().getId().equals(conditionAvro.getSensorId());

        // Сравниваем значение
        Object newValue = conditionAvro.getValue();
        int expectedValue;
        if (newValue instanceof Integer intValue) {
            expectedValue = intValue;
        } else if (newValue instanceof Boolean boolValue) {
            expectedValue = boolValue ? 1 : 0;
        } else {
            return false;
        }

        boolean valueMatches = condition.getValue() == expectedValue;

        return typeMatches && operationMatches && sensorMatches && valueMatches;
    }

    private boolean matchesAction(ScenarioAction scenarioAction, DeviceActionAvro actionAvro) {
        Action action = scenarioAction.getAction();

        // Сравниваем тип, значение и датчик
        boolean typeMatches = action.getType().name().equals(actionAvro.getType().name());
        boolean valueMatches = action.getValue() == actionAvro.getValue();
        boolean sensorMatches = scenarioAction.getSensor().getId().equals(actionAvro.getSensorId());

        return typeMatches && valueMatches && sensorMatches;
    }

    private Map<String, Sensor> loadAndValidateSensors(String hubId, String scenarioName, ScenarioAddedEventAvro event) {
        // Собираем все ID датчиков из условий и действий
        Set<String> allSensorIds = new HashSet<>();
        event.getConditions().forEach(c -> allSensorIds.add(c.getSensorId()));
        event.getActions().forEach(a -> allSensorIds.add(a.getSensorId()));

        if (allSensorIds.isEmpty()) {
            return new HashMap<>();
        }

        // Получаем все датчики одним запросом
        Map<String, Sensor> sensorsMap = sensorRepository.findAllById(allSensorIds).stream()
                .collect(Collectors.toMap(Sensor::getId, Function.identity()));

        // Проверяем, что все датчики найдены
        for (String sensorId : allSensorIds) {
            if (!sensorsMap.containsKey(sensorId)) {
                log.error("Cannot add/update scenario '{}' for hub '{}': sensor '{}' not found. " +
                                "Required sensors: {}, Found sensors: {}",
                        scenarioName, hubId, sensorId, allSensorIds, sensorsMap.keySet());
                throw new IllegalStateException("Sensor not found: " + sensorId +
                        ". Ensure all sensors are added before creating scenario.");
            }
        }

        return sensorsMap;
    }
}