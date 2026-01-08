package ru.yandex.practicum.telemetry.analyzer.configuration;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import ru.yandex.practicum.kafka.telemetry.configuration.KafkaListenerFactoryBuilder;
import ru.yandex.practicum.kafka.telemetry.deserializer.HubEventDeserializer;
import ru.yandex.practicum.kafka.telemetry.deserializer.SnapshotDeserializer;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

/**
 * Конфигурация Kafka для сервиса Analyzer.
 * Создает два listener factory с разными deserializer'ами.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class KafkaConfiguration {

    private static final String OFFSET_RESET_LATEST = "latest";

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, HubEventAvro> hubEventListenerFactory(
            KafkaProperties kafkaProperties) {
        return KafkaListenerFactoryBuilder.create(kafkaProperties, HubEventDeserializer.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SensorsSnapshotAvro> snapshotListenerFactory(
            KafkaProperties kafkaProperties) {
        return KafkaListenerFactoryBuilder.create(
                kafkaProperties,
                SnapshotDeserializer.class,
                OFFSET_RESET_LATEST
        );
    }
}