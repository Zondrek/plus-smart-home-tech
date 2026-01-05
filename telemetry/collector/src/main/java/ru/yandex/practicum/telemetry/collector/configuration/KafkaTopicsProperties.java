package ru.yandex.practicum.telemetry.collector.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Конфигурация топиков Kafka для сервиса Collector.
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
     * Топик для событий хабов.
     */
    private String hubs;
}