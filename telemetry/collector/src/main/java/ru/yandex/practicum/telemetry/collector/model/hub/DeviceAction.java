package ru.yandex.practicum.telemetry.collector.model.hub;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.yandex.practicum.telemetry.collector.model.enums.ActionType;

@Getter
@Setter
@ToString
@Builder
public class DeviceAction {
    private String sensorId;
    private ActionType type;
    private Integer value;
}
