package ru.yandex.practicum.telemetry.analyzer.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "kafka.topic")
public class KafkaTopicsProperties {
    private String hubs;
    private String snapshots;
}