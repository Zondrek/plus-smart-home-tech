package ru.yandex.practicum.telemetry.collector.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties("collector.kafka")
public class KafkaConfigurationProperties {

    private Producer producer = new Producer();
    private Map<String, String> topics = new HashMap<>();

    @Getter
    @Setter
    public static class Producer {
        private Map<String, String> properties = new HashMap<>();
    }
}