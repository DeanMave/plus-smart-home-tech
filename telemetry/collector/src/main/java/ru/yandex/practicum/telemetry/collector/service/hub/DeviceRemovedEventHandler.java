package ru.yandex.practicum.telemetry.collector.service.hub;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceRemovedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.collector.service.TelemetryKafkaProducer;
import ru.yandex.practicum.telemetry.collector.service.mapper.AvroMapper;

@Component
public class DeviceRemovedEventHandler extends BaseHubEventHandler {

    public DeviceRemovedEventHandler(AvroMapper avroMapper, TelemetryKafkaProducer kafkaProducer) {
        super(avroMapper, kafkaProducer);
    }

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.DEVICE_REMOVED_EVENT;
    }

    @Override
    public void handle(HubEventProto event) {
        DeviceRemovedEventProto protoPayload = event.getDeviceRemovedEvent();
        DeviceRemovedEventAvro avroPayload = avroMapper.toDeviceRemovedEventAvro(protoPayload);
        HubEventAvro hubEventAvro = avroMapper.toHubEventAvroContainer(
                event,
                avroPayload
        );
        kafkaProducer.sendHubEvent(hubEventAvro);
    }
}