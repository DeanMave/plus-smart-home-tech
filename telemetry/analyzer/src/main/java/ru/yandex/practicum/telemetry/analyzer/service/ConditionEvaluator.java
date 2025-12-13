package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.model.Condition;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConditionEvaluator {
    private final ScenarioRepository scenarioRepository;

    @Transactional(readOnly = true)
    public List<Scenario> evaluateSnapshot(String hubId, SensorsSnapshotAvro sensorsSnapshotAvro) {
        List<Scenario> allScenarios = scenarioRepository.findByHubId(hubId);
        List<Scenario> triggeredScenarios = new ArrayList<>();

        for (Scenario scenario : allScenarios) {
            if (isScenarioTriggered(scenario, sensorsSnapshotAvro)) {
                triggeredScenarios.add(scenario);
                log.info("Сценарий '{}' для хаба {} активирован", scenario.getName(), hubId);
            }
        }
        return triggeredScenarios;
    }

    private boolean isScenarioTriggered(Scenario scenario, SensorsSnapshotAvro snapshot) {
        Map<String, Condition> conditions = scenario.getConditions();
        if (conditions.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Condition> conditionEntry : conditions.entrySet()) {
            String sensorId = conditionEntry.getKey();
            Condition condition = conditionEntry.getValue();
            if (!checkCondition(sensorId, condition, snapshot)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkCondition(String sensorId, Condition condition, SensorsSnapshotAvro snapshot) {
        SensorStateAvro sensorState = findSensorInSnapshot(sensorId, snapshot);
        if (sensorState == null) {
            log.warn("Сенсор {} не найден в снапшоте", sensorId);
            return false;
        }
        Integer sensorValue = extractNumericValue(sensorState, condition.getType());
        if (sensorValue == null) {
            log.warn("Не удалось получить значение для типа условия {} из сенсора {}", condition.getType(), sensorId);
            return false;
        }
        return checkConditionValue(sensorValue, condition.getOperation(), condition.getValue());
    }

    private SensorStateAvro findSensorInSnapshot(String sensorId, SensorsSnapshotAvro snapshot) {
        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();
        if (sensorsState == null) {
            return null;
        }
        return sensorsState.get(sensorId);
    }

    private Integer extractNumericValue(SensorStateAvro sensorState, ConditionTypeAvro conditionType) {
        Object data = sensorState.getData();

        switch (conditionType) {
            case TEMPERATURE:
                if (data instanceof ClimateSensorAvro) {
                    return ((ClimateSensorAvro) data).getTemperatureC();
                } else if (data instanceof TemperatureSensorAvro) {
                    return ((TemperatureSensorAvro) data).getTemperatureC();
                }
                break;
            case HUMIDITY:
                if (data instanceof ClimateSensorAvro) {
                    return ((ClimateSensorAvro) data).getHumidity();
                }
                break;
            case CO2LEVEL:
                if (data instanceof ClimateSensorAvro) {
                    return ((ClimateSensorAvro) data).getCo2Level();
                }
                break;
            case LUMINOSITY:
                if (data instanceof LightSensorAvro) {
                    return ((LightSensorAvro) data).getLuminosity();
                }
                break;
            case MOTION:
                if (data instanceof MotionSensorAvro) {
                    return ((MotionSensorAvro) data).getMotion() ? 1 : 0;
                }
                break;
            case SWITCH:
                if (data instanceof SwitchSensorAvro) {
                    return ((SwitchSensorAvro) data).getState() ? 1 : 0;
                }
                break;
        }
        return null;
    }

    private boolean checkConditionValue(Integer sensorValue, ConditionOperationAvro operation, Integer conditionValue) {
        if (sensorValue == null || conditionValue == null) {
            return false;
        }

        switch (operation) {
            case EQUALS:
                return sensorValue.equals(conditionValue);
            case GREATER_THAN:
                return sensorValue > conditionValue;
            case LOWER_THAN:
                return sensorValue < conditionValue;
            default:
                log.warn("Неизвестная операция: {}", operation);
                return false;
        }
    }
}