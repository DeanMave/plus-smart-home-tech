package ru.yandex.practicum.telemetry.collector.service.sensor;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.ClimateSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.service.TelemetryKafkaProducer;
import ru.yandex.practicum.telemetry.collector.service.mapper.AvroMapper;

@Component
public class ClimateSensorEventHandler extends BaseSensorEventHandler {
    public ClimateSensorEventHandler(AvroMapper avroMapper, TelemetryKafkaProducer kafkaProducer) {
        super(avroMapper, kafkaProducer);
    }

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.CLIMATE_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        ClimateSensorProto climateProto = event.getClimateSensor();

        ClimateSensorAvro climateAvroPayload = avroMapper.toClimateSensorAvro(climateProto);

        SensorEventAvro sensorEventAvro = avroMapper.toSensorEventAvroContainer(
                event,
                climateAvroPayload
        );

        kafkaProducer.sendSensorEvent(sensorEventAvro);
    }
}
