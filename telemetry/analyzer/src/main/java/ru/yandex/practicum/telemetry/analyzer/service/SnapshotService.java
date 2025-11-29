package ru.yandex.practicum.telemetry.analyzer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

@Service
public class SnapshotService {

    @Transactional(readOnly = true)
    public void processSnapshot(SensorsSnapshotAvro snapshot, ScenarioExecutor executor) {
        executor.executeScenarios(snapshot);
    }
}