package ru.yandex.practicum.kafka.telemetry.deserializer;

import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.serialization.BaseAvroDeserializer;

/**
 * Десериализатор для событий датчиков.
 * Используется для чтения событий из топика Kafka.
 */
public class SensorEventDeserializer extends BaseAvroDeserializer<SensorEventAvro> {

    /**
     * Конструктор по умолчанию.
     * Использует схему класса SensorEventAvro для десериализации.
     */
    public SensorEventDeserializer() {
        super(SensorEventAvro.getClassSchema());
    }
}