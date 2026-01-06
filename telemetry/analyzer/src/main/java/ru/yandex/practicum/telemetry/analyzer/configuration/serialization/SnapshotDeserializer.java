package ru.yandex.practicum.telemetry.analyzer.configuration.serialization;

import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.serialization.BaseAvroDeserializer;

/**
 * Десериализатор для снапшотов состояния датчиков.
 * Используется для чтения снапшотов из топика Kafka.
 */
public class SnapshotDeserializer extends BaseAvroDeserializer<SensorsSnapshotAvro> {

    /**
     * Конструктор по умолчанию.
     * Использует схему класса SensorsSnapshotAvro для десериализации.
     */
    public SnapshotDeserializer() {
        super(SensorsSnapshotAvro.getClassSchema());
    }
}