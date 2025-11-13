package ru.yandex.practicum.telemetry.collector.service.hub;

import ru.yandex.practicum.telemetry.collector.service.TelemetryKafkaProducer;
import ru.yandex.practicum.telemetry.collector.service.mapper.AvroMapper;

public abstract class BaseHubEventHandler implements HubEventHandler {
    protected final AvroMapper avroMapper;
    protected final TelemetryKafkaProducer kafkaProducer;

    protected BaseHubEventHandler(AvroMapper avroMapper, TelemetryKafkaProducer kafkaProducer) {
        this.avroMapper = avroMapper;
        this.kafkaProducer = kafkaProducer;
    }
}
