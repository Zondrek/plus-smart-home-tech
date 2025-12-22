package ru.yandex.practicum.telemetry.collector.service.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.collector.model.sensor.*;

@Component
public class SensorEventMapper {

    public SensorEventAvro toAvro(SensorEvent event) {
        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(mapPayload(event))
                .build();
    }

    private Object mapPayload(SensorEvent event) {
        return switch (event.getType()) {
            case LIGHT_SENSOR_EVENT -> mapLightSensor((LightSensorEvent) event);
            case MOTION_SENSOR_EVENT -> mapMotionSensor((MotionSensorEvent) event);
            case TEMPERATURE_SENSOR_EVENT -> mapTemperatureSensor((TemperatureSensorEvent) event);
            case CLIMATE_SENSOR_EVENT -> mapClimateSensor((ClimateSensorEvent) event);
            case SWITCH_SENSOR_EVENT -> mapSwitchSensor((SwitchSensorEvent) event);
        };
    }

    private LightSensorAvro mapLightSensor(LightSensorEvent event) {
        return LightSensorAvro.newBuilder()
                .setLinkQuality(event.getLinkQuality() != null ? event.getLinkQuality() : 0)
                .setLuminosity(event.getLuminosity() != null ? event.getLuminosity() : 0)
                .build();
    }

    private MotionSensorAvro mapMotionSensor(MotionSensorEvent event) {
        return MotionSensorAvro.newBuilder()
                .setLinkQuality(event.getLinkQuality())
                .setMotion(event.getMotion())
                .setVoltage(event.getVoltage())
                .build();
    }

    private TemperatureSensorAvro mapTemperatureSensor(TemperatureSensorEvent event) {
        return TemperatureSensorAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setTemperatureC(event.getTemperatureC())
                .setTemperatureF(event.getTemperatureF())
                .build();
    }

    private ClimateSensorAvro mapClimateSensor(ClimateSensorEvent event) {
        return ClimateSensorAvro.newBuilder()
                .setTemperatureC(event.getTemperatureC())
                .setHumidity(event.getHumidity())
                .setCo2Level(event.getCo2Level())
                .build();
    }

    private SwitchSensorAvro mapSwitchSensor(SwitchSensorEvent event) {
        return SwitchSensorAvro.newBuilder()
                .setState(event.getState())
                .build();
    }
}