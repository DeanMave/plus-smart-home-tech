package ru.yandex.practicum.telemetry.collector.service.mapper;

import org.mapstruct.*;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.collector.model.enums.HubEventType;
import ru.yandex.practicum.telemetry.collector.model.hub.*;
import ru.yandex.practicum.telemetry.collector.model.sensor.*;

@Mapper(componentModel = "spring")
public interface AvroMapper {
    @Mapping(source = "hubId", target = "hubId")
    @Mapping(target = "payload", source = ".")
    HubEventAvro toHubEventAvro(HubEvent hubEvent);

    @Mapping(source = "sensorId", target = "sensorId")
    DeviceActionAvro toDeviceActionAvro(DeviceAction deviceAction);

    @Mapping(target = "type", ignore = true)
    DeviceAddedEventAvro toDeviceAddedEventAvro(DeviceAddedEvent deviceAddedEvent);

    default Object mapHubEventType(HubEventType type) {
        return null;
    }

    DeviceRemovedEventAvro toDeviceRemovedEventAvro(DeviceRemovedEvent deviceRemovedEvent);

    @Mapping(source = "sensorId", target = "sensorId")
    ScenarioConditionAvro toScenarioConditionAvro(ScenarioCondition scenarioCondition);

    ScenarioAddedEventAvro toScenarioAddedEventAvro(ScenarioAddedEvent scenarioAddedEvent);

    ScenarioRemovedEventAvro toScenarioRemovedEventAvro(ScenarioRemovedEvent scenarioRemovedEvent);

    @Mapping(target = "payload", source = "sensorEvent")
    SensorEventAvro toSensorEventAvro(SensorEvent sensorEvent);

    @Mapping(source = "temperatureC", target = "temperatureC")
    @Mapping(source = "co2Level", target = "co2Level")
    ClimateSensorAvro toClimateSensorAvro(ClimateSensorEvent climateSensorEvent);

    @Mapping(source = "linkQuality", target = "linkQuality")
    LightSensorAvro toLightSensorAvro(LightSensorEvent lightSensorEvent);

    MotionSensorAvro toMotionSensorAvro(MotionSensorEvent motionSensorEvent);

    SwitchSensorAvro toSwitchSensorAvro(SwitchSensorEvent switchSensorEvent);

    TemperatureSensorAvro toTemperatureSensorAvro(TemperatureSensorEvent temperatureSensorEvent);
}
