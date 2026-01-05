package ru.yandex.practicum.telemetry.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Маппер для преобразования gRPC proto-событий хабов в Avro-формат.
 */
@Component
public class HubEventMapper {

    /**
     * Преобразует proto-событие хаба в avro-событие.
     */
    public HubEventAvro mapToAvro(HubEventProto proto) {
        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(proto.getHubId())
                .setTimestamp(Instant.ofEpochSecond(proto.getTimestamp().getSeconds(), proto.getTimestamp().getNanos()));

        // Определяем тип события по payload
        switch (proto.getPayloadCase()) {
            case DEVICE_ADDED:
                builder.setPayload(mapDeviceAdded(proto.getDeviceAdded()));
                break;
            case DEVICE_REMOVED:
                builder.setPayload(mapDeviceRemoved(proto.getDeviceRemoved()));
                break;
            case SCENARIO_ADDED:
                builder.setPayload(mapScenarioAdded(proto.getScenarioAdded()));
                break;
            case SCENARIO_REMOVED:
                builder.setPayload(mapScenarioRemoved(proto.getScenarioRemoved()));
                break;
            default:
                throw new IllegalArgumentException("Unknown hub event type: " + proto.getPayloadCase());
        }

        return builder.build();
    }

    private DeviceAddedEventAvro mapDeviceAdded(DeviceAddedEventProto proto) {
        return DeviceAddedEventAvro.newBuilder()
                .setId(proto.getId())
                .setDeviceType(mapDeviceType(proto.getType()))
                .build();
    }

    private DeviceRemovedEventAvro mapDeviceRemoved(DeviceRemovedEventProto proto) {
        return DeviceRemovedEventAvro.newBuilder()
                .setId(proto.getId())
                .build();
    }

    private ScenarioAddedEventAvro mapScenarioAdded(ScenarioAddedEventProto proto) {
        return ScenarioAddedEventAvro.newBuilder()
                .setName(proto.getName())
                .setConditions(proto.getConditionsList().stream()
                        .map(this::mapScenarioCondition)
                        .collect(Collectors.toList()))
                .setActions(proto.getActionsList().stream()
                        .map(this::mapDeviceAction)
                        .collect(Collectors.toList()))
                .build();
    }

    private ScenarioRemovedEventAvro mapScenarioRemoved(ScenarioRemovedEventProto proto) {
        return ScenarioRemovedEventAvro.newBuilder()
                .setName(proto.getName())
                .build();
    }

    private ScenarioConditionAvro mapScenarioCondition(ScenarioConditionProto proto) {
        ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(mapConditionType(proto.getType()))
                .setOperation(mapConditionOperation(proto.getOperation()));

        // Обрабатываем oneof value: может быть bool_value или int_value
        switch (proto.getValueCase()) {
            case BOOL_VALUE:
                builder.setValue(proto.getBoolValue());
                break;
            case INT_VALUE:
                builder.setValue(proto.getIntValue());
                break;
            case VALUE_NOT_SET:
                builder.setValue(null);
                break;
        }

        return builder.build();
    }

    private DeviceActionAvro mapDeviceAction(DeviceActionProto proto) {
        DeviceActionAvro.Builder builder = DeviceActionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(mapActionType(proto.getType()));

        // Обрабатываем optional value
        if (proto.hasValue()) {
            builder.setValue(proto.getValue());
        } else {
            builder.setValue(null);
        }

        return builder.build();
    }

    private DeviceTypeAvro mapDeviceType(DeviceTypeProto proto) {
        switch (proto) {
            case MOTION_SENSOR: return DeviceTypeAvro.MOTION_SENSOR;
            case TEMPERATURE_SENSOR: return DeviceTypeAvro.TEMPERATURE_SENSOR;
            case LIGHT_SENSOR: return DeviceTypeAvro.LIGHT_SENSOR;
            case CLIMATE_SENSOR: return DeviceTypeAvro.CLIMATE_SENSOR;
            case SWITCH_SENSOR: return DeviceTypeAvro.SWITCH_SENSOR;
            default: throw new IllegalArgumentException("Unknown device type: " + proto);
        }
    }

    private ActionTypeAvro mapActionType(ActionTypeProto proto) {
        switch (proto) {
            case ACTIVATE: return ActionTypeAvro.ACTIVATE;
            case DEACTIVATE: return ActionTypeAvro.DEACTIVATE;
            case INVERSE: return ActionTypeAvro.INVERSE;
            case SET_VALUE: return ActionTypeAvro.SET_VALUE;
            default: throw new IllegalArgumentException("Unknown action type: " + proto);
        }
    }

    private ConditionTypeAvro mapConditionType(ConditionTypeProto proto) {
        switch (proto) {
            case MOTION: return ConditionTypeAvro.MOTION;
            case LUMINOSITY: return ConditionTypeAvro.LUMINOSITY;
            case SWITCH: return ConditionTypeAvro.SWITCH;
            case TEMPERATURE: return ConditionTypeAvro.TEMPERATURE;
            case CO2LEVEL: return ConditionTypeAvro.CO2LEVEL;
            case HUMIDITY: return ConditionTypeAvro.HUMIDITY;
            default: throw new IllegalArgumentException("Unknown condition type: " + proto);
        }
    }

    private ConditionOperationAvro mapConditionOperation(ConditionOperationProto proto) {
        switch (proto) {
            case EQUALS: return ConditionOperationAvro.EQUALS;
            case GREATER_THAN: return ConditionOperationAvro.GREATER_THAN;
            case LOWER_THAN: return ConditionOperationAvro.LOWER_THAN;
            default: throw new IllegalArgumentException("Unknown condition operation: " + proto);
        }
    }
}