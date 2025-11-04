package ru.yandex.practicum.telemetry.collector.model.hub;

import lombok.*;
import ru.yandex.practicum.telemetry.collector.model.enums.ActionType;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAction {
    private String sensorId;
    private ActionType type;
    private Integer value;
}
