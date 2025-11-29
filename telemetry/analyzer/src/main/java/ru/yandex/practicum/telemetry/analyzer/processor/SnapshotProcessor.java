package ru.yandex.practicum.telemetry.analyzer.processor;

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
import ru.yandex.practicum.telemetry.analyzer.config.KafkaAnalyzerConfig;
import ru.yandex.practicum.telemetry.analyzer.service.ScenarioExecutor;

import java.time.Duration;
import java.util.*;


@Slf4j
@Component
public class SnapshotProcessor implements Runnable {

    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private final KafkaConsumer<String, SensorsSnapshotAvro> consumer;
    private final List<String> topics;
    private final Duration pollTimeout;
    private final ScenarioExecutor scenarioExecutor;

    public SnapshotProcessor(KafkaAnalyzerConfig config, ScenarioExecutor scenarioExecutor) {

        String consumerType = this.getClass().getSimpleName();

        KafkaAnalyzerConfig.ConsumerConfigItem consumerConfig = config.getConsumerConfig(consumerType);

        this.consumer = new KafkaConsumer<>(config.getConsumerProperties(consumerType));
        this.topics = consumerConfig.getTopics();
        this.pollTimeout = consumerConfig.getPollTimeout();
        this.scenarioExecutor = scenarioExecutor;


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Сработал хук на завершение JVM. Прерываю работу консьюмера SnapshotProcessor.");
            consumer.wakeup();
        }));
    }

    @Override
    public void run() {
        start();
    }

    public void start() {
        try {
            log.info("SnapshotProcessor запущен. Подписываемся на топики: {}", topics);
            consumer.subscribe(topics);

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(pollTimeout);

                if (records.isEmpty()) {
                    continue;
                }

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    log.trace("Обработка снапшота хаба {} из партиции {} с офсетом {}.",
                            record.key(), record.partition(), record.offset());

                    processRecord(record.value());

                    updateOffsets(record);
                }

                if (!currentOffsets.isEmpty()) {
                    consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                        if (exception != null) {
                            log.warn("Ошибка во время асинхронной фиксации оффсетов: {}", offsets, exception);
                        } else {
                            log.debug("Асинхронно зафиксированы оффсеты: {}", offsets);
                        }
                    });
                    currentOffsets.clear();
                }
            }
        } catch (WakeupException ignores) {
            log.info("Получен сигнал завершения работы. SnapshotProcessor остановлен.");
        } catch (Exception e) {
            log.error("Критическая ошибка во время обработки снапшотов", e);
        } finally {
            log.info("Попытка синхронной фиксации оставшихся оффсетов перед закрытием.");
            try {
                consumer.commitSync();
            } finally {
                log.info("Закрываем SnapshotProcessor Consumer");
                consumer.close();
            }
        }
    }

    @Transactional(readOnly = true)
    private void processRecord(SensorsSnapshotAvro snapshot) {
        scenarioExecutor.executeScenarios(snapshot);
    }

    private void updateOffsets(ConsumerRecord<String, SensorsSnapshotAvro> record) {
        TopicPartition tp = new TopicPartition(record.topic(), record.partition());
        currentOffsets.put(tp, new OffsetAndMetadata(record.offset() + 1));
    }
}