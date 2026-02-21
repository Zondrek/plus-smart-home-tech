package ru.yandex.practicum.telemetry.aggregator.configuration;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import ru.yandex.practicum.kafka.telemetry.configuration.KafkaListenerFactoryBuilder;
import ru.yandex.practicum.kafka.telemetry.deserializer.SensorEventDeserializer;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

/**
 * Конфигурация Kafka для сервиса Aggregator.
 * Создает listener factory для обработки событий датчиков.
 */
@Configuration
public class KafkaConfiguration {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SensorEventAvro> sensorEventListenerFactory(
            KafkaProperties kafkaProperties) {
        return KafkaListenerFactoryBuilder.create(kafkaProperties, SensorEventDeserializer.class);
    }
}