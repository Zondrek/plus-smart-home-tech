package ru.yandex.practicum.telemetry.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.collector.model.hub.*;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Маппер для преобразования proto-событий и DTO хабов в Avro-формат.
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
                .setType(mapDeviceType(proto.getType()))
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

    private DeviceTypeAvro mapDeviceType(Object type) {
        return DeviceTypeAvro.valueOf(type.toString());
    }

    private ActionTypeAvro mapActionType(Object type) {
        return ActionTypeAvro.valueOf(type.toString());
    }

    private ConditionTypeAvro mapConditionType(Object type) {
        return ConditionTypeAvro.valueOf(type.toString());
    }

    private ConditionOperationAvro mapConditionOperation(Object operation) {
        return ConditionOperationAvro.valueOf(operation.toString());
    }
}