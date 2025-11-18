package ru.yandex.practicum.telemetry.collector.service.hub;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioRemovedEventProto;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;
import ru.yandex.practicum.telemetry.collector.service.TelemetryKafkaProducer;
import ru.yandex.practicum.telemetry.collector.service.mapper.AvroMapper;

@Component
public class ScenarioRemovedEventHandler extends BaseHubEventHandler {

    public ScenarioRemovedEventHandler(AvroMapper avroMapper, TelemetryKafkaProducer kafkaProducer) {
        super(avroMapper, kafkaProducer);
    }

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.SCENARIO_REMOVED_EVENT;
    }

    @Override
    public void handle(HubEventProto event) {
        ScenarioRemovedEventProto protoPayload = event.getScenarioRemovedEvent();

        ScenarioRemovedEventAvro avroPayload = avroMapper.toScenarioRemovedEventAvro(protoPayload);

        HubEventAvro hubEventAvro = avroMapper.toHubEventAvroContainer(
                event,
                avroPayload
        );

        kafkaProducer.sendHubEvent(hubEventAvro);
    }
}