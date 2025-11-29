package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import com.google.protobuf.Timestamp;
import ru.yandex.practicum.telemetry.analyzer.entity.Action;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
public class ActionSender {

    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public ActionSender(@GrpcClient("hub-router") HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient) {
        this.hubRouterClient = hubRouterClient;
    }

    public void sendAction(String hubId, String scenarioName, Map<String, Action> actions) {
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();

        for (Map.Entry<String, Action> entry : actions.entrySet()) {
            String sensorId = entry.getKey();
            Action action = entry.getValue();

            DeviceActionProto.Builder actionBuilder = DeviceActionProto.newBuilder()
                    .setSensorId(sensorId)
                    .setType(ActionTypeProto.valueOf(action.getType().name()));

            if (action.getValue() != null) {
                actionBuilder.setValue(action.getValue());
            }

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(hubId)
                    .setScenarioName(scenarioName)
                    .setAction(actionBuilder.build())
                    .setTimestamp(timestamp)
                    .build();

            try {
                log.info("Отправка команды хабу {} для сценария '{}', устройство: {}",
                        hubId, scenarioName, sensorId);
                // RPC вызов
                hubRouterClient.handleDeviceAction(request);
            } catch (Exception e) {
                log.error("Ошибка при отправке хабу {} действия {} для устройства {}",
                        hubId, action.getType(), sensorId, e);
            }
        }
    }
}