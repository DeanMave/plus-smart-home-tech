package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.entity.Action;
import ru.yandex.practicum.telemetry.analyzer.entity.Condition;
import ru.yandex.practicum.telemetry.analyzer.entity.Scenario;
import ru.yandex.practicum.telemetry.analyzer.entity.Sensor;
import ru.yandex.practicum.telemetry.analyzer.repository.ActionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.SensorRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @Transactional
    public void handleDeviceAdded(String hubId, DeviceAddedEventAvro event) {
        String sensorId = event.getId();

        if (sensorRepository.existsById(sensorId)) {
            log.trace("Сенсор {} уже существует, игнорирую DEVICE_ADDED.", sensorId);
            return;
        }

        Sensor sensor = new Sensor();
        sensor.setId(sensorId);
        sensor.setHubId(hubId);

        sensorRepository.save(sensor);
        log.info("Добавлен сенсор {} для хаба {}", sensorId, hubId);
    }

    @Transactional
    public void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro event) {
        String sensorId = event.getId();
        sensorRepository.deleteById(sensorId);
        log.info("Удален сенсор {} для хаба {}", sensorId, hubId);
    }

    @Transactional
    public void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro event) {
        String scenarioName = event.getName();

        Optional<Scenario> scenarioOptional = scenarioRepository.findByHubIdAndName(hubId, scenarioName);

        if (scenarioOptional.isPresent()) {
            Scenario scenario = scenarioOptional.get();

            if (!scenario.getConditions().isEmpty()) {
                scenario.getConditions().values().forEach(conditionRepository::delete);
            }

            if (!scenario.getActions().isEmpty()) {
                scenario.getActions().values().forEach(actionRepository::delete);
            }

            scenarioRepository.delete(scenario);
            log.info("Удален сценарий '{}' для хаба {}. Очищены связанные условия и действия.", scenarioName, hubId);
        } else {
            log.warn("Сценарий '{}' для хаба {} не найден. Удаление не требуется.", scenarioName, hubId);
        }
    }

    @Transactional
    public void handleScenarioAdded(String hubId, ScenarioAddedEventAvro event) {
        String scenarioName = event.getName();
        Optional<Scenario> existing = scenarioRepository.findByHubIdAndName(hubId, scenarioName);

        if (existing.isPresent()) {
            log.info("Сценарий '{}' для хаба {} уже существует, удаляем старую версию перед сохранением.", scenarioName, hubId);

            Scenario oldScenario = existing.get();

            if (!oldScenario.getConditions().isEmpty()) {
                oldScenario.getConditions().values().forEach(conditionRepository::delete);
                log.debug("Удалено {} старых условий для сценария '{}'.", oldScenario.getConditions().size(), scenarioName);
            }

            if (!oldScenario.getActions().isEmpty()) {
                oldScenario.getActions().values().forEach(actionRepository::delete);
                log.debug("Удалено {} старых действий для сценария '{}'.", oldScenario.getActions().size(), scenarioName);
            }

            scenarioRepository.delete(oldScenario);
        }

        Scenario scenario = new Scenario();
        scenario.setHubId(hubId);
        scenario.setName(scenarioName);

        Map<String, Condition> newConditionsMap = new HashMap<>();
        Map<String, Action> newActionsMap = new HashMap<>();

        for (ScenarioConditionAvro avro : event.getConditions()) {
            Condition condition = new Condition();

            condition.setType(avro.getType());
            condition.setOperation(avro.getOperation());
            condition.setValue(extractValue(avro.getValue()));

            Condition savedCondition = conditionRepository.save(condition);

            newConditionsMap.put(avro.getSensorId(), savedCondition);
        }
        scenario.setConditions(newConditionsMap);

        for (DeviceActionAvro avro : event.getActions()) {
            Action action = new Action();

            action.setType(avro.getType());
            action.setValue(extractValue(avro.getValue()));

            Action savedAction = actionRepository.save(action);

            newActionsMap.put(avro.getSensorId(), savedAction);
        }
        scenario.setActions(newActionsMap);

        scenarioRepository.save(scenario);

        log.info("Сценарий '{}' для хаба {} успешно сохранен/обновлен с {} условиями и {} действиями.",
                scenarioName, hubId, newConditionsMap.size(), newActionsMap.size());
    }

    private Integer extractValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        return null;
    }
}