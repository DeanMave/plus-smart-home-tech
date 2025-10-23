package ru.yandex.practicum.telemetry.collector.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Service
public class TelemetryKafkaProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final String hubTopic;
    private final String sensorTopic;

    public TelemetryKafkaProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topic.hub}") String hubTopic,
            @Value("${kafka.topic.sensor}") String sensorTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;

        this.hubTopic = hubTopic;
        this.sensorTopic = sensorTopic;
    }

    public void sendSensorEvent(SensorEventAvro event) {
        String key = event.getHubId();

        kafkaTemplate.send(sensorTopic, key, event);

    }

    public void sendHubEvent(HubEventAvro event) {
        String key = event.getHubId();

        kafkaTemplate.send(hubTopic, key, event);
    }
}
