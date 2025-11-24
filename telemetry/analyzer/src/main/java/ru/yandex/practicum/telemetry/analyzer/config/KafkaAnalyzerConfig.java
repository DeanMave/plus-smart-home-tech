package ru.yandex.practicum.telemetry.analyzer.config;

import lombok.Data;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Data
@Component
@ConfigurationProperties(prefix = "analyzer.kafka")
public class KafkaAnalyzerConfig {

    private Map<String, Object> commonProperties = new HashMap<>();
    private List<ConsumerConfigItem> consumers;

    public Properties getConsumerProperties(String consumerType) {
        ConsumerConfigItem configItem = consumers.stream()
                .filter(c -> consumerType.equals(c.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Consumer config not found for type: " + consumerType));

        Properties props = new Properties();
        props.putAll(commonProperties);
        props.putAll(configItem.getProperties());

        if (!props.containsKey(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG)) {
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    "org.apache.kafka.common.serialization.StringDeserializer");
        }

        return props;
    }

    public ConsumerConfigItem getConsumerConfig(String consumerType) {
        return consumers.stream()
                .filter(c -> consumerType.equals(c.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Consumer config not found for type: " + consumerType));
    }

    @Data
    public static class ConsumerConfigItem {
        private String type;
        private Map<String, Object> properties = new HashMap<>();
        private List<String> topics;
        private Duration pollTimeout;
    }
}