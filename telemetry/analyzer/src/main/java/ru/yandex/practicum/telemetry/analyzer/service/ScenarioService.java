package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.model.Action;
import ru.yandex.practicum.telemetry.analyzer.model.Condition;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;
import ru.yandex.practicum.telemetry.analyzer.model.Sensor;
import ru.yandex.practicum.telemetry.analyzer.repository.ActionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.SensorRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @Transactional
    public void upsertScenario(String hubId, ScenarioAddedEventAvro scenarioAddedEvent) {
        log.info("Начало обработки сценария '{}' для Hub_id: {}", scenarioAddedEvent.getName(), hubId);

        Optional<Scenario> existingScenario = scenarioRepository.findByHubIdAndName(hubId, scenarioAddedEvent.getName());
        if (existingScenario.isPresent()) {
            log.warn("Сценарий '{}' (Hub {}) уже существует. Удаление старой версии перед обновлением", scenarioAddedEvent.getName(), hubId);
            scenarioRepository.delete(existingScenario.get());
        }

        Scenario scenario = new Scenario();
        scenario.setHubId(hubId);
        scenario.setName(scenarioAddedEvent.getName());

        Map<String, Condition> conditions = mapAndSaveConditions(hubId, scenarioAddedEvent.getConditions());
        scenario.setConditions(conditions);

        Map<String, Action> actions = mapAndSaveActions(hubId, scenarioAddedEvent.getActions());
        scenario.setActions(actions);

        scenarioRepository.save(scenario);
        log.info("Сценарий '{}' для Hub {} успешно сохранен в БД", scenarioAddedEvent.getName(), hubId);
    }

    @Transactional
    public void removeScenario (String hubId, ScenarioRemovedEventAvro scenarioRemovedEvent) {
        log.info("Попытка удаления сценария '{}' для Hub ID: {}", scenarioRemovedEvent.getName(), hubId);

        Optional<Scenario> scenario = scenarioRepository.findByHubIdAndName(hubId, scenarioRemovedEvent.getName());
        if (scenario.isPresent()) {
            scenarioRepository.delete(scenario.get());
            log.info("Сценарий '{}' (Hub {}) успешно удален", scenarioRemovedEvent.getName(), hubId);
        } else {
            log.warn("Сценарий '{}' для Hub {} не найден в базе данных", scenarioRemovedEvent.getName(), hubId);
        }
    }

    @Transactional
    public void registerDevice(String hubId, DeviceAddedEventAvro deviceAddedEvent) {
        log.info("Регистрация устройства {} (тип {}) для Hub ID: {}",
                deviceAddedEvent.getId(), deviceAddedEvent.getType(), hubId);

        Optional<Sensor> existingSensor = sensorRepository.findById(deviceAddedEvent.getId());
        if (existingSensor.isPresent()) {
            log.warn("Устройство {} уже зарегистрировано", deviceAddedEvent.getId());
            return;
        }

        Sensor sensor = new Sensor();
        sensor.setId(deviceAddedEvent.getId());
        sensor.setHubId(hubId);
        sensorRepository.save(sensor);

        log.info("Устройство {} для Hub {} успешно сохранено", deviceAddedEvent.getId(), hubId);
    }

    @Transactional
    public void removeDevice(String hubId, DeviceRemovedEventAvro deviceRemovedEvent) {
        log.info("Попытка удаления устройства {} для Hub ID: {}", deviceRemovedEvent.getId(), hubId);

        Optional<Sensor> sensor = sensorRepository.findById(deviceRemovedEvent.getId());
        if (sensor.isPresent()) {
            if (!sensor.get().getHubId().equals(hubId)) {
                log.warn("Устройство {} не принадлежит заявленному хабу {}", deviceRemovedEvent.getId(), hubId);
                return;
            }

            sensorRepository.delete(sensor.get());
            log.info("Устройство {} (Hub {}) успешно удалено", deviceRemovedEvent.getId(), hubId);
        } else {
            log.warn("Устройство {} не найдено в базе данных", deviceRemovedEvent.getId());
        }
    }


    private Map<String, Condition> mapAndSaveConditions(String hubId, List<ScenarioConditionAvro> conditionsAvro) {
        Map<String, Condition> conditions = new HashMap<>();

        for (ScenarioConditionAvro conditionAvro : conditionsAvro) {
            Optional<Sensor> sensor = sensorRepository.findByIdAndHubId(conditionAvro.getSensorId(), hubId);
            if (sensor.isEmpty()) {
                log.warn("Сенсор {} для Hub {} не найден. Условие пропущено.", conditionAvro.getSensorId(), hubId);
                continue;
            }

            Condition condition = new Condition();
            condition.setType(conditionAvro.getType());
            condition.setOperation(conditionAvro.getOperation());

            Integer value = extractValue(conditionAvro.getValue());
            condition.setValue(value);

            Condition savedCondition = conditionRepository.save(condition);
            conditions.put(conditionAvro.getSensorId(), savedCondition);
        }

        return conditions;
    }

    private Map<String, Action> mapAndSaveActions(String hubId, List<DeviceActionAvro> actionsAvro) {
        Map<String, Action> actions = new HashMap<>();

        for (DeviceActionAvro actionAvro : actionsAvro) {
            Optional<Sensor> sensor = sensorRepository.findByIdAndHubId(actionAvro.getSensorId(), hubId);
            if (sensor.isEmpty()) {
                log.warn("Сенсор {} для Hub {} не найден. Действие пропущено.", actionAvro.getSensorId(), hubId);
                continue;
            }

            Action action = new Action();
            action.setType(actionAvro.getType());

            Integer value = extractValue(actionAvro.getValue());
            action.setValue(value);

            Action savedAction = actionRepository.save(action);
            actions.put(actionAvro.getSensorId(), savedAction);
        }

        return actions;
    }

    private Integer extractValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        return null;
    }

}