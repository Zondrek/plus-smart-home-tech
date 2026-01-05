package ru.yandex.practicum.telemetry.aggregator.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Конфигурация топиков Kafka для сервиса Aggregator.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "kafka.topic")
public class KafkaTopicsProperties {

    /**
     * Топик для событий датчиков.
     */
    private String sensors;

    /**
     * Топик для снапшотов состояния датчиков.
     */
    private String snapshots;
}