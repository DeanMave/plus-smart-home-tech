package ru.yandex.practicum.telemetry.collector.service.sensor;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.LightSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.service.TelemetryKafkaProducer;
import ru.yandex.practicum.telemetry.collector.service.mapper.AvroMapper;

@Component
public class LightSensorEventHandler extends BaseSensorEventHandler {
    public LightSensorEventHandler(AvroMapper avroMapper, TelemetryKafkaProducer kafkaProducer) {
        super(avroMapper, kafkaProducer);
    }


    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.LIGHT_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        LightSensorProto lightProto = event.getLightSensor();

        LightSensorAvro lightAvroPayload = avroMapper.toLightSensorAvro(lightProto);

        SensorEventAvro sensorEventAvro = avroMapper.toSensorEventAvroContainer(
                event,
                lightAvroPayload
        );

        kafkaProducer.sendSensorEvent(sensorEventAvro);
    }
}
