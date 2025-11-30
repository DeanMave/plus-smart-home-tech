package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.analyzer.config.KafkaConsumerConfig;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SnapshotProcessor {
    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    private final KafkaConsumer<String, SensorsSnapshotAvro> consumer;
    private final List<String> topics;
    private final Duration pollTimeout;
    private final ConditionEvaluator conditionEvaluator;
    private final ActionCommandSender actionCommandSender;

    public SnapshotProcessor(KafkaConsumerConfig config, ConditionEvaluator conditionEvaluator, ActionCommandSender actionCommandSender) {
        final KafkaConsumerConfig.ConsumerConfig consumerConfig = config.getConsumers().get(this.getClass().getSimpleName());
        this.consumer = new KafkaConsumer<>(consumerConfig.getProperties());
        this.topics = consumerConfig.getTopics();
        this.pollTimeout = consumerConfig.getPollTimeout();
        this.conditionEvaluator = conditionEvaluator;
        this.actionCommandSender = actionCommandSender;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Сработал хук на завершение JVM. Прерываю работу консьюмера.");
            consumer.wakeup();
        }));
    }

    public void start() {
        try{
            log.trace("Консьюмер подписывается на топики: {}", topics);
            consumer.subscribe(topics);
            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(pollTimeout);
                int count = 0;
                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    log.trace("Получена запись: Hub_id {}, Partition {}, Offset {}",
                            record.key(), record.partition(), record.offset());
                    handleRecord(record.value());
                    manageOffsets(record, count);
                    count++;
                }
                consumer.commitAsync();
            }
        } catch (WakeupException ignores) {
            log.info("Получен сигнал завершения работы (WakeupException) в Snapshot Processor");
        } catch (Exception e) {
            log.error("Общая ошибка в цикле обработки снапшотов", e);
        } finally {
            try {
                consumer.commitSync(currentOffsets);
            } finally {
                log.info("Завершение работы и закрытие консьюмера");
                consumer.close();
            }
        }
    }

    private void manageOffsets(ConsumerRecord<String, SensorsSnapshotAvro> record, int count) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );
        if(count % 100 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if(exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }

    @Transactional
    private void handleRecord(SensorsSnapshotAvro sensorsSnapshotAvro) {
        try {
            String hubId = sensorsSnapshotAvro.getHubId();
            List<Scenario> scenarios = conditionEvaluator.evaluateSnapshot(hubId, sensorsSnapshotAvro);
            for (Scenario scenario : scenarios) {
                actionCommandSender.executeScenarioActions(scenario);
            }
        } catch (Exception e) {
            log.error("Не удалось обработать снапшот {}", sensorsSnapshotAvro, e);
        }
    }
}
