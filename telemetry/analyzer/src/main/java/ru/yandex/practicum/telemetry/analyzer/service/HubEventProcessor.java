package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.config.KafkaConsumerConfig;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HubEventProcessor implements Runnable {

    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    private final KafkaConsumer<String, HubEventAvro> consumer;
    private final List<String> topics;
    private final Duration pollTimeout;
    private final ScenarioService scenarioService;

    public HubEventProcessor(KafkaConsumerConfig config, ScenarioService scenarioService) {
        final KafkaConsumerConfig.ConsumerConfig consumerConfig = config.getConsumers().get(this.getClass().getSimpleName());
        this.consumer = new KafkaConsumer<>(consumerConfig.getProperties());
        this.topics = consumerConfig.getTopics();
        this.pollTimeout = consumerConfig.getPollTimeout();
        this.scenarioService = scenarioService;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Сработал хук на завершение JVM. Прерываю работу консьюмера.");
            consumer.wakeup();
        }));
    }

    @Override
    public void run() {
        try{
            log.trace("Консьюмер подписывается на топики: {}", topics);
            consumer.subscribe(topics);
            while (true) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(pollTimeout);
                int count = 0;
                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    log.trace("Получена запись: Hub_id {}, Partition {}, Offset {}.",
                            record.key(), record.partition(), record.offset());
                    handleRecord(record.value());
                    manageOffsets(record, count);
                    count++;
                }
                consumer.commitAsync();
            }
        } catch (WakeupException ignores) {
            log.info("Получен сигнал завершения работы (WakeupException) в Hub Event Processor.");
        } catch (Exception e) {
            log.error("Общая ошибка в цикле обработки событий хаба.", e);
        } finally {
            try {
                consumer.commitSync(currentOffsets);
            } finally {
                log.info("Завершение работы и закрытие консьюмера.");
                consumer.close();
            }
        }

    }

    private void manageOffsets(ConsumerRecord<String, HubEventAvro> record, int count) {
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

    private void handleRecord(HubEventAvro hubEventAvro) {
        try {
            String hubId = hubEventAvro.getHubId();
            switch (hubEventAvro.getPayload()) {
                case DeviceAddedEventAvro dae -> scenarioService.registerDevice(hubId, dae);
                case DeviceRemovedEventAvro dre -> scenarioService.removeDevice(hubId, dre);
                case ScenarioAddedEventAvro sae -> scenarioService.upsertScenario(hubId, sae);
                case ScenarioRemovedEventAvro sre -> scenarioService.removeScenario(hubId, sre);
                default -> log.warn("Неизвестный тип события: {}", hubEventAvro);
            }
        } catch (Exception e) {
            log.error("Ошибка обработки payload для Hub ID {}", hubEventAvro.getHubId(), e);
        }
    }
}