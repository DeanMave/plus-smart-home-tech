package ru.yandex.practicum.telemetry.collector.model.hub;

import lombok.*;
import ru.yandex.practicum.telemetry.collector.model.enums.ConditionOperation;
import ru.yandex.practicum.telemetry.collector.model.enums.ConditionType;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioCondition {
    private String sensorId;
    private ConditionType type;
    private ConditionOperation operation;
    private Integer value;
}
