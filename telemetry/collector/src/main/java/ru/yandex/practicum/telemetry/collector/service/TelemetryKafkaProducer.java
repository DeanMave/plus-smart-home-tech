package ru.yandex.practicum.telemetry.collector.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.serializer.GeneralAvroSerializer;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.util.Properties;

@Service
public class TelemetryKafkaProducer {
    private KafkaProducer<String, SpecificRecordBase> producer;

    @Value("${kafka.topic.hub}")
    private String hubTopic;
    @Value("${kafka.topic.sensor}")
    private String sensorTopic;
    @Value("${kafka.bootstrap-servers}")
    private String bootstrap;
    @Value("${kafka.producer.acks}")
    private String acksConfig;
    @Value("${kafka.producer.retries}")
    private String retriesConfig;
    @Value("${kafka.producer.max-in-flight-requests-per-connection}")
    private String maxInFlightRequests;
    @Value("${kafka.producer.linger-ms}")
    private String lingerMsConfig;
    @Value("${kafka.producer.batch-size}")
    private String batchSizeConfig;

    @PostConstruct
    public void init() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GeneralAvroSerializer.class.getName());

        properties.put(ProducerConfig.ACKS_CONFIG, acksConfig);
        properties.put(ProducerConfig.RETRIES_CONFIG, retriesConfig);
        properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxInFlightRequests);

        properties.put(ProducerConfig.LINGER_MS_CONFIG, lingerMsConfig);
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSizeConfig);
        this.producer = new KafkaProducer<>(properties);
    }

    @PreDestroy
    public void close() {
        if (producer != null) {
            producer.close();
        }
    }

    public void sendHubEvent(HubEventAvro event) {
        String key = event.getHubId();
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(hubTopic, key, event);
        producer.send(record);
    }

    public void sendSensorEvent(SensorEventAvro event) {
        String key = event.getHubId();
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(sensorTopic, key, event);
        producer.send(record);
    }
}
