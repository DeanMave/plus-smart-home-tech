package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.config.KafkaAnalyzerConfig;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class HubEventProcessor implements Runnable {

    private final KafkaConsumer<String, HubEventAvro> consumer;
    private final List<String> topics;
    private final Duration pollTimeout;
    private final ScenarioService scenarioService;

    public HubEventProcessor(KafkaAnalyzerConfig config, ScenarioService scenarioService) {
        String consumerType = this.getClass().getSimpleName();
        KafkaAnalyzerConfig.ConsumerConfigItem consumerConfig = config.getConsumerConfig(consumerType);

        this.consumer = new KafkaConsumer<>(config.getConsumerProperties(consumerType));
        this.topics = consumerConfig.getTopics();
        this.pollTimeout = consumerConfig.getPollTimeout();
        this.scenarioService = scenarioService;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Прерываю работу консьюмера HubEventProcessor.");
            consumer.wakeup();
        }));
    }

    @Override
    public void run() {
        try {
            log.info("HubEventProcessor запущен. Подписываемся на топики: {}", topics);
            consumer.subscribe(topics);

            while (true) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(pollTimeout);

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    handleRecord(record.value());
                }

                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException ignores) {
            log.info("HubEventProcessor остановлен.");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий хабов", e);
        } finally {
            log.info("Закрываем HubEventProcessor Consumer");
            consumer.close();
        }
    }

    private void handleRecord(HubEventAvro hubEventAvro) {
        try {
            String hubId = hubEventAvro.getHubId();
            switch (hubEventAvro.getPayload()) {
                case DeviceAddedEventAvro dae -> scenarioService.handleDeviceAdded(hubId, dae);
                case DeviceRemovedEventAvro dre -> scenarioService.handleDeviceRemoved(hubId, dre);
                case ScenarioAddedEventAvro sae -> scenarioService.handleScenarioAdded(hubId, sae);
                case ScenarioRemovedEventAvro sre -> scenarioService.handleScenarioRemoved(hubId, sre);
                default ->
                        log.warn("Неизвестный тип события: {}", hubEventAvro.getPayload().getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("Ошибка обработки события для хаба {}", e);
        }
    }
}