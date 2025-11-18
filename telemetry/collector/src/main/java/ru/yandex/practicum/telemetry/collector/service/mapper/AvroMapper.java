package ru.yandex.practicum.telemetry.collector.service.mapper;

import org.apache.avro.specific.SpecificRecordBase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;
import com.google.protobuf.Timestamp;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface AvroMapper {

    @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.NULL)
    DeviceTypeAvro toDeviceTypeAvro(DeviceTypeProto protoType);

    @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.NULL)
    ConditionTypeAvro toConditionTypeAvro(ConditionTypeProto proto);

    @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.NULL)
    ConditionOperationAvro toConditionOperationAvro(ConditionOperationProto proto);

    @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.NULL)
    ActionTypeAvro toActionTypeAvro(ActionTypeProto proto);

    @Mapping(target = "type", source = "type")
    DeviceAddedEventAvro toDeviceAddedEventAvro(DeviceAddedEventProto deviceAddedEvent);

    DeviceRemovedEventAvro toDeviceRemovedEventAvro(DeviceRemovedEventProto deviceRemovedEvent);

    @Mapping(source = "conditionList", target = "conditions")
    @Mapping(source = "actionList", target = "actions")
    ScenarioAddedEventAvro toScenarioAddedEventAvro(ScenarioAddedEventProto scenarioAddedEvent);

    ScenarioRemovedEventAvro toScenarioRemovedEventAvro(ScenarioRemovedEventProto scenarioRemovedEvent);

    default Object mapScenarioConditionValue(ScenarioConditionProto proto) {
        switch (proto.getValueCase()) {
            case BOOL_VALUE:
                return proto.getBoolValue();
            case INT_VALUE:
                return proto.getIntValue();
            case VALUE_NOT_SET:
            default:
                return null;
        }
    }

    @Mapping(target = "value", source = "proto")
    ScenarioConditionAvro toScenarioConditionAvro(ScenarioConditionProto proto);

    DeviceActionAvro toDeviceActionAvro(DeviceActionProto deviceAction);

    TemperatureSensorAvro toCompleteTemperatureSensorAvro(TemperatureSensorProto sensorEvent);

    ClimateSensorAvro toClimateSensorAvro(ClimateSensorProto climateSensorEvent);

    LightSensorAvro toLightSensorAvro(LightSensorProto lightSensorEvent);

    MotionSensorAvro toMotionSensorAvro(MotionSensorProto motionSensorEvent);

    SwitchSensorAvro toSwitchSensorAvro(SwitchSensorProto switchSensorEvent);

    default Instant mapTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    @Mapping(target = "timestamp", source = "event.timestamp")
    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "hubId", source = "event.hubId")
    @Mapping(target = "payload", source = "payload")
    SensorEventAvro toSensorEventAvroContainer(SensorEventProto event, SpecificRecordBase payload);

    @Mapping(target = "timestamp", source = "event.timestamp")
    @Mapping(target = "hubId", source = "event.hubId")
    @Mapping(target = "payload", source = "payload")
    HubEventAvro toHubEventAvroContainer(HubEventProto event, SpecificRecordBase payload);
}