package ru.yandex.practicum.telemetry.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotServiceImpl implements SnapshotService {
    private final Map<String, SensorsSnapshotAvro> sensorsSnapshotAvroMap = new HashMap<>();

    @Override
    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        final SensorsSnapshotAvro snapshot = sensorsSnapshotAvroMap.computeIfAbsent(
                event.getHubId(),
                hubId -> SensorsSnapshotAvro.newBuilder()
                        .setHubId(hubId)
                        .setTimestamp(event.getTimestamp())
                        .setSensorsState(new HashMap<>())
                        .build()
        );
        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();

        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();

        if (sensorsState.containsKey(event.getId())) {
            SensorStateAvro oldState = sensorsState.get(event.getId());
            if (!event.getTimestamp().isAfter(oldState.getTimestamp())) {
                if (oldState.getData().equals(newState.getData())) {
                    return Optional.empty();
                }
            }

            if (oldState.getData().equals(newState.getData())) {
                return Optional.empty();
            }
        }
        sensorsState.put(event.getId(), newState);
        snapshot.setTimestamp(newState.getTimestamp());
        return Optional.of(snapshot);
    }
}
