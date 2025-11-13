package ru.yandex.practicum.telemetry.collector.service.hub;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceAddedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.collector.service.TelemetryKafkaProducer;
import ru.yandex.practicum.telemetry.collector.service.mapper.AvroMapper;

@Component
public class DeviceAddedEventHandler extends BaseHubEventHandler {

    public DeviceAddedEventHandler(AvroMapper avroMapper, TelemetryKafkaProducer kafkaProducer) {
        super(avroMapper, kafkaProducer);
    }

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.DEVICE_ADDED_EVENT;
    }

    @Override
    public void handle(HubEventProto event) {
        DeviceAddedEventProto protoPayload = event.getDeviceAddedEvent();
        DeviceAddedEventAvro avroPayload = avroMapper.toDeviceAddedEventAvro(protoPayload);
        HubEventAvro hubEventAvro = avroMapper.toHubEventAvroContainer(
                event,
                avroPayload
        );
        kafkaProducer.sendHubEvent(hubEventAvro);
    }
}