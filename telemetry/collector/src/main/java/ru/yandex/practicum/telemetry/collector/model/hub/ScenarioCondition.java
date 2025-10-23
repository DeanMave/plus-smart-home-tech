package ru.yandex.practicum.telemetry.collector.model.hub;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.yandex.practicum.telemetry.collector.model.enums.ConditionOperation;
import ru.yandex.practicum.telemetry.collector.model.enums.ConditionType;

@Getter
@Setter
@ToString
@Builder
public class ScenarioCondition {
    private String sensorId;
    private ConditionType type;
    private ConditionOperation operation;
    private Integer value;
}
