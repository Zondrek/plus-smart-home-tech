package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.model.*;
import ru.yandex.practicum.telemetry.analyzer.repository.*;

import java.util.ArrayList;
import java.util.List;

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
                .orElse(new Scenario());

        scenario.setHubId(hubId);
        scenario.setName(scenarioName);
        scenario.setConditions(new ArrayList<>());
        scenario.setActions(new ArrayList<>());

        // Сохраняем сценарий, чтобы получить ID
        scenario = scenarioRepository.save(scenario);

        // Обрабатываем условия
        for (ScenarioConditionAvro conditionAvro : event.getConditions()) {
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

            condition = conditionRepository.save(condition);

            ScenarioConditionId id = new ScenarioConditionId(
                    scenario.getId(),
                    conditionAvro.getSensorId(),
                    condition.getId()
            );

            Sensor sensor = sensorRepository.findById(conditionAvro.getSensorId())
                    .orElseThrow(() -> new IllegalStateException("Sensor not found: " + conditionAvro.getSensorId()));

            ScenarioCondition scenarioCondition = new ScenarioCondition();
            scenarioCondition.setId(id);
            scenarioCondition.setScenario(scenario);
            scenarioCondition.setSensor(sensor);
            scenarioCondition.setCondition(condition);

            scenario.getConditions().add(scenarioCondition);
        }

        // Обрабатываем действия
        for (DeviceActionAvro actionAvro : event.getActions()) {
            Action action = new Action();
            action.setType(ActionType.valueOf(actionAvro.getType().name()));
            action.setValue(actionAvro.getValue());

            action = actionRepository.save(action);

            ScenarioActionId id = new ScenarioActionId(
                    scenario.getId(),
                    actionAvro.getSensorId(),
                    action.getId()
            );

            Sensor sensor = sensorRepository.findById(actionAvro.getSensorId())
                    .orElseThrow(() -> new IllegalStateException("Sensor not found: " + actionAvro.getSensorId()));

            ScenarioAction scenarioAction = new ScenarioAction();
            scenarioAction.setId(id);
            scenarioAction.setScenario(scenario);
            scenarioAction.setSensor(sensor);
            scenarioAction.setAction(action);

            scenario.getActions().add(scenarioAction);
        }

        scenarioRepository.save(scenario);
        log.info("Added/Updated scenario: {} for hub: {}", scenarioName, hubId);
    }

    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro event) {
        String scenarioName = event.getName();

        scenarioRepository.findByHubIdAndName(hubId, scenarioName).ifPresent(scenario -> {
            scenarioRepository.delete(scenario);
            log.info("Removed scenario: {} from hub: {}", scenarioName, hubId);
        });
    }
}