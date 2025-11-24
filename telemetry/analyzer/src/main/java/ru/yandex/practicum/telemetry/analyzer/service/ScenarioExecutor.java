package ru.yandex.practicum.telemetry.analyzer.service;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.entity.Action;
import ru.yandex.practicum.telemetry.analyzer.entity.Condition;
import ru.yandex.practicum.telemetry.analyzer.entity.Scenario;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;


import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ScenarioExecutor {

    private final ScenarioRepository scenarioRepository;
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public ScenarioExecutor(ScenarioRepository scenarioRepository,
                            @GrpcClient("hub-router") HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient) {
        this.scenarioRepository = scenarioRepository;
        this.hubRouterClient = hubRouterClient;
    }

    @Transactional
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
                executeScenarioActions(scenario, hubId);
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

        for (Map.Entry<String, Condition> entry : conditions.entrySet()) {
            String sensorId = entry.getKey();
            Condition condition = entry.getValue();

            SensorStateAvro sensorState = sensorStates.get(sensorId);
            if (sensorState == null) {
                log.warn("Сенсор {} не найден в снапшоте. Условие не выполнено.", sensorId);
                return false;
            }

            Integer sensorValue = getSensorValue(sensorState, condition.getType());
            if (sensorValue == null) {
                log.warn("Не удалось получить значение для типа условия {} из сенсора {}", condition.getType(), sensorId);
                return false;
            }

            if (!checkConditionValue(sensorValue, condition.getOperation(), condition.getValue())) {
                return false;
            }
        }
        return true;
    }

    private Integer getSensorValue(SensorStateAvro sensorState, ConditionTypeAvro conditionType) {
        Object data = sensorState.getData();

        switch (conditionType) {
            case TEMPERATURE:
                if (data instanceof ClimateSensorAvro c) {
                    return c.getTemperatureC();
                } else if (data instanceof TemperatureSensorAvro t) {
                    return t.getTemperatureC();
                }
                break;
            case HUMIDITY:
                if (data instanceof ClimateSensorAvro c) {
                    return c.getHumidity();
                }
                break;
            case CO2LEVEL:
                if (data instanceof ClimateSensorAvro c) {
                    return c.getCo2Level();
                }
                break;
            case LUMINOSITY:
                if (data instanceof LightSensorAvro l) {
                    return l.getLuminosity();
                }
                break;
            case MOTION:
                if (data instanceof MotionSensorAvro m) {
                    return m.getMotion() ? 1 : 0;
                }
                break;
            case SWITCH:
                if (data instanceof SwitchSensorAvro s) {
                    return s.getState() ? 1 : 0;
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

    private void executeScenarioActions(Scenario scenario, String hubId) {
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();

        for (Map.Entry<String, Action> entry : scenario.getActions().entrySet()) {
            String sensorId = entry.getKey();
            Action action = entry.getValue();

            DeviceActionProto.Builder actionBuilder = DeviceActionProto.newBuilder()
                    .setSensorId(sensorId)
                    .setType(ActionTypeProto.valueOf(action.getType().name()));

            if (action.getType().equals(ActionTypeAvro.SET_VALUE)) {
                actionBuilder.setValue(action.getValue());
            }

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(hubId)
                    .setScenarioName(scenario.getName())
                    .setAction(actionBuilder.build())
                    .setTimestamp(timestamp)
                    .build();

            try {
                log.info("Отправка команды хабу {} для сценария {}", hubId, scenario.getName());
                hubRouterClient.handleDeviceAction(request); // <-- Используем метод из примера
            } catch (Exception e) {
                log.error("Ошибка при отправке хабу {} действия {} для устройства {}",
                        hubId, action, sensorId, e);
            }
        }
    }
}