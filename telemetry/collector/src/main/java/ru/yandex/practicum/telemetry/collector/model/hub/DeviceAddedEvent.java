package ru.yandex.practicum.telemetry.collector.model.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.yandex.practicum.telemetry.collector.model.enums.DeviceType;
import ru.yandex.practicum.telemetry.collector.model.enums.HubEventType;

@Getter
@Setter
@ToString(callSuper = true)
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAddedEvent extends HubEvent {
    @NotBlank
    private String id;
    @NotNull
    private DeviceType deviceType;

    @Override
    public HubEventType getType() {
        return HubEventType.DEVICE_ADDED_EVENT;
    }
}
