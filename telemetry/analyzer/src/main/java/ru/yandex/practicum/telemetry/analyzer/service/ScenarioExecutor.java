package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.entity.Condition;
import ru.yandex.practicum.telemetry.analyzer.entity.Scenario;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.util.ConditionOperation;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ScenarioExecutor {

    private final ScenarioRepository scenarioRepository;
    private final ActionSender actionSender;
    private final SensorValueExtractor valueExtractor;

    public ScenarioExecutor(ScenarioRepository scenarioRepository,
                            ActionSender actionSender,
                            SensorValueExtractor valueExtractor) {
        this.scenarioRepository = scenarioRepository;
        this.actionSender = actionSender;
        this.valueExtractor = valueExtractor;
    }

    public void executeScenarios(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        if (scenarios.isEmpty()) {
            log.trace("Сценарии для хаба {} не найдены, анализ завершен.", hubId);
            return;
        }

        log.info("Найдено {} сценариев для анализа для хаба {}", scenarios.size(), hubId);

        scenarios.forEach(scenario -> {
            if (checkScenarioConditions(scenario, snapshot)) {
                log.info("Сценарий '{}' для хаба {} выполнен! Выполняем действия.", scenario.getName(), hubId);
                actionSender.sendAction(hubId, scenario.getName(), scenario.getActions());
            } else {
                log.trace("Сценарий '{}' не выполнен.", scenario.getName());
            }
        });
    }

    private boolean checkScenarioConditions(Scenario scenario, SensorsSnapshotAvro snapshot) {
        Map<String, Condition> conditions = scenario.getConditions();
        Map<String, SensorStateAvro> sensorStates = snapshot.getSensorsState();

        if (conditions.isEmpty()) {
            return true;
        }

        return conditions.entrySet().stream().allMatch(entry -> {
            String sensorId = entry.getKey();
            Condition condition = entry.getValue();

            SensorStateAvro sensorState = sensorStates.get(sensorId);
            if (sensorState == null) {
                log.warn("Сенсор {} не найден в снапшоте. Условие не выполнено.", sensorId);
                return false;
            }

            Integer sensorValue = valueExtractor.extract(sensorState, condition.getType());
            if (sensorValue == null) {
                log.warn("Не удалось получить значение для типа условия {} из сенсора {}", condition.getType(), sensorId);
                return false;
            }
            try {
                ConditionOperation operation = ConditionOperation.fromAvro(condition.getOperation());

                if (!operation.evaluate(sensorValue, condition.getValue())) {
                    log.trace("Условие не выполнено: {} {} {} (Фактическое: {})",
                            condition.getType(), condition.getOperation(), condition.getValue(), sensorValue);
                    return false;
                }
                return true;
            } catch (Exception e) {
                log.error("Ошибка при проверке условия: {}", e.getMessage());
                return false;
            }
        });
    }

}