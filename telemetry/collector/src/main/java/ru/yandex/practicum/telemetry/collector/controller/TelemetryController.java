package ru.yandex.practicum.telemetry.collector.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.telemetry.collector.service.TelemetryKafkaProducer;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.service.mapper.AvroMapper;

@RestController
@AllArgsConstructor
@RequestMapping("/events")
public class TelemetryController {
    private AvroMapper mapper;
    private TelemetryKafkaProducer producer;

    @PostMapping("/sensors")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        producer.sendSensorEvent(mapper.toSensorEventAvro(event));
    }

    @PostMapping("/hubs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void collectHubEvent(@Valid @RequestBody HubEvent event) {
        producer.sendHubEvent(mapper.toHubEventAvro(event));
    }
}
