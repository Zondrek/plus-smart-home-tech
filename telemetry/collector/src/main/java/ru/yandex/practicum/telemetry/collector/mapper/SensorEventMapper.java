package ru.yandex.practicum.telemetry.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

/**
 * Маппер для преобразования gRPC proto-событий датчиков в Avro-формат.
 */
@Component
public class SensorEventMapper {

    /**
     * Преобразует proto-событие датчика в avro-событие.
     */
    public SensorEventAvro mapToAvro(SensorEventProto proto) {
        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                .setId(proto.getId())
                .setHubId(proto.getHubId())
                .setTimestamp(Instant.ofEpochSecond(proto.getTimestamp().getSeconds(), proto.getTimestamp().getNanos()));

        // Определяем тип события по payload
        switch (proto.getPayloadCase()) {
            case MOTION_SENSOR:
                builder.setPayload(mapMotionSensor(proto.getMotionSensor()));
                break;
            case TEMPERATURE_SENSOR:
                builder.setPayload(mapTemperatureSensor(proto));
                break;
            case LIGHT_SENSOR:
                builder.setPayload(mapLightSensor(proto.getLightSensor()));
                break;
            case CLIMATE_SENSOR:
                builder.setPayload(mapClimateSensor(proto.getClimateSensor()));
                break;
            case SWITCH_SENSOR:
                builder.setPayload(mapSwitchSensor(proto.getSwitchSensor()));
                break;
            default:
                throw new IllegalArgumentException("Unknown sensor event type: " + proto.getPayloadCase());
        }

        return builder.build();
    }

    private MotionSensorAvro mapMotionSensor(MotionSensorProto proto) {
        return MotionSensorAvro.newBuilder()
                .setLinkQuality(proto.getLinkQuality())
                .setMotion(proto.getMotion())
                .setVoltage(proto.getVoltage())
                .build();
    }

    private TemperatureSensorAvro mapTemperatureSensor(SensorEventProto proto) {
        return TemperatureSensorAvro.newBuilder()
                .setId(proto.getId())
                .setHubId(proto.getHubId())
                .setTimestamp(Instant.ofEpochSecond(proto.getTimestamp().getSeconds(), proto.getTimestamp().getNanos()))
                .setTemperatureC(proto.getTemperatureSensor().getTemperatureC())
                .setTemperatureF(proto.getTemperatureSensor().getTemperatureF())
                .build();
    }

    private LightSensorAvro mapLightSensor(LightSensorProto proto) {
        return LightSensorAvro.newBuilder()
                .setLinkQuality(proto.getLinkQuality())
                .setLuminosity(proto.getLuminosity())
                .build();
    }

    private ClimateSensorAvro mapClimateSensor(ClimateSensorProto proto) {
        return ClimateSensorAvro.newBuilder()
                .setTemperatureC(proto.getTemperatureC())
                .setHumidity(proto.getHumidity())
                .setCo2Level(proto.getCo2Level())
                .build();
    }

    private SwitchSensorAvro mapSwitchSensor(SwitchSensorProto proto) {
        return SwitchSensorAvro.newBuilder()
                .setState(proto.getState())
                .build();
    }
}