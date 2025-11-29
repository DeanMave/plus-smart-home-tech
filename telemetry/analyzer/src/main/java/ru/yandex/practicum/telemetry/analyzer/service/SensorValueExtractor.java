package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;


@Component
@Slf4j
public class SensorValueExtractor {

    public Integer extract(SensorStateAvro sensorState, ConditionTypeAvro conditionType) {
        Object data = sensorState.getData();

        if (data == null) {
            return null;
        }

        switch (conditionType) {
            case TEMPERATURE:
                if (data instanceof ClimateSensorAvro c) {
                    return c.getTemperatureC();
                } else if (data instanceof TemperatureSensorAvro t) {
                    return t.getTemperatureC();
                }
                break;
            case HUMIDITY:
                if (data instanceof ClimateSensorAvro c) {
                    return c.getHumidity();
                }
                break;
            case CO2LEVEL:
                if (data instanceof ClimateSensorAvro c) {
                    return c.getCo2Level();
                }
                break;
            case LUMINOSITY:
                if (data instanceof LightSensorAvro l) {
                    return l.getLuminosity();
                }
                break;
            case MOTION:
                if (data instanceof MotionSensorAvro m) {
                    return m.getMotion() ? 1 : 0;
                }
                break;
            case SWITCH:
                if (data instanceof SwitchSensorAvro s) {
                    return s.getState() ? 1 : 0;
                }
                break;
        }

        log.warn("Не удалось извлечь значение типа {} из данных сенсора: {}", conditionType, data.getClass().getSimpleName());
        return null;
    }
}