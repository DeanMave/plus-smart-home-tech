package ru.yandex.practicum.telemetry.collector.service.sensor;

import ru.yandex.practicum.telemetry.collector.service.TelemetryKafkaProducer;
import ru.yandex.practicum.telemetry.collector.service.mapper.AvroMapper;

public abstract class BaseSensorEventHandler implements SensorEventHandler {
    protected final AvroMapper avroMapper;
    protected final TelemetryKafkaProducer kafkaProducer;

    protected BaseSensorEventHandler(AvroMapper avroMapper, TelemetryKafkaProducer kafkaProducer) {
        this.avroMapper = avroMapper;
        this.kafkaProducer = kafkaProducer;
    }
}
