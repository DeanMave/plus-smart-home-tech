package ru.yandex.practicum.telemetry.collector.service.hub;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioAddedEventProto;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.telemetry.collector.service.TelemetryKafkaProducer;
import ru.yandex.practicum.telemetry.collector.service.mapper.AvroMapper;

@Component
public class ScenarioAddedEventHandler extends BaseHubEventHandler {

    public ScenarioAddedEventHandler(AvroMapper avroMapper, TelemetryKafkaProducer kafkaProducer) {
        super(avroMapper, kafkaProducer);
    }

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.SCENARIO_ADDED_EVENT;
    }

    @Override
    public void handle(HubEventProto event) {
        ScenarioAddedEventProto protoPayload = event.getScenarioAddedEvent();
        ScenarioAddedEventAvro avroPayload = avroMapper.toScenarioAddedEventAvro(protoPayload);
        HubEventAvro hubEventAvro = avroMapper.toHubEventAvroContainer(
                event,
                avroPayload
        );
        kafkaProducer.sendHubEvent(hubEventAvro);
    }
}