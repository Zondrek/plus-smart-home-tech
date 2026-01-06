package ru.yandex.practicum.telemetry.analyzer.configuration.serialization;

import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.serialization.BaseAvroDeserializer;

/**
 * Десериализатор для событий хабов.
 * Используется для чтения событий из топика Kafka.
 */
public class HubEventDeserializer extends BaseAvroDeserializer<HubEventAvro> {

    /**
     * Конструктор по умолчанию.
     * Использует схему класса HubEventAvro для десериализации.
     */
    public HubEventDeserializer() {
        super(HubEventAvro.getClassSchema());
    }
}