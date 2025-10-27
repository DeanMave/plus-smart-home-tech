package ru.yandex.practicum.telemetry.collector.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.telemetry.collector.model.hub.*;
import ru.yandex.practicum.telemetry.collector.model.sensor.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Mapper(componentModel = "spring")
public interface AvroMapper {

    @Mapping(source = "hubEvent", target = "payload")
    HubEventAvro toHubEventAvro(HubEvent hubEvent);

    @Mapping(source = "sensorEvent", target = "payload")
    SensorEventAvro toSensorEventAvro(SensorEvent sensorEvent);


    default Object mapPayload(HubEvent hubEvent) {
        if (hubEvent instanceof DeviceAddedEvent) {
            return toDeviceAddedEventAvro((DeviceAddedEvent) hubEvent);
        }
        if (hubEvent instanceof DeviceRemovedEvent) {
            return toDeviceRemovedEventAvro((DeviceRemovedEvent) hubEvent);
        }
        if (hubEvent instanceof ScenarioAddedEvent) {
            return toScenarioAddedEventAvro((ScenarioAddedEvent) hubEvent);
        }
        if (hubEvent instanceof ScenarioRemovedEvent) {
            return toScenarioRemovedEventAvro((ScenarioRemovedEvent) hubEvent);
        }
        return null;
    }

    default Object mapPayload(SensorEvent sensorEvent) {
        if (sensorEvent instanceof ClimateSensorEvent) {
            return toClimateSensorAvro((ClimateSensorEvent) sensorEvent);
        }
        if (sensorEvent instanceof LightSensorEvent) {
            return toLightSensorAvro((LightSensorEvent) sensorEvent);
        }
        if (sensorEvent instanceof MotionSensorEvent) {
            return toMotionSensorAvro((MotionSensorEvent) sensorEvent);
        }
        if (sensorEvent instanceof SwitchSensorEvent) {
            return toSwitchSensorAvro((SwitchSensorEvent) sensorEvent);
        }
        if (sensorEvent instanceof TemperatureSensorEvent) {
            return toCompleteTemperatureSensorAvro((TemperatureSensorEvent) sensorEvent);
        }
        return null;
    }

    @Mapping(target = "type", source = "deviceType")
    DeviceAddedEventAvro toDeviceAddedEventAvro(DeviceAddedEvent deviceAddedEvent);

    DeviceRemovedEventAvro toDeviceRemovedEventAvro(DeviceRemovedEvent deviceRemovedEvent);

    ScenarioAddedEventAvro toScenarioAddedEventAvro(ScenarioAddedEvent scenarioAddedEvent);

    ScenarioRemovedEventAvro toScenarioRemovedEventAvro(ScenarioRemovedEvent scenarioRemovedEvent);

    ScenarioConditionAvro toScenarioConditionAvro(ScenarioCondition scenarioCondition);

    DeviceActionAvro toDeviceActionAvro(DeviceAction deviceAction);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "hubId", target = "hubId")
    @Mapping(source = "timestamp", target = "timestamp")
    @Mapping(source = "temperatureC", target = "temperatureC")
    @Mapping(source = "temperatureF", target = "temperatureF")
    TemperatureSensorAvro toCompleteTemperatureSensorAvro(TemperatureSensorEvent sensorEvent);

    ClimateSensorAvro toClimateSensorAvro(ClimateSensorEvent climateSensorEvent);

    LightSensorAvro toLightSensorAvro(LightSensorEvent lightSensorEvent);

    MotionSensorAvro toMotionSensorAvro(MotionSensorEvent motionSensorEvent);

    SwitchSensorAvro toSwitchSensorAvro(SwitchSensorEvent switchSensorEvent);
}