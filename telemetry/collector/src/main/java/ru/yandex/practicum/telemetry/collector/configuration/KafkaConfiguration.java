package ru.yandex.practicum.telemetry.collector.configuration;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.configuration.serialization.AvroSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация Kafka для сервиса Collector.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
@RequiredArgsConstructor
public class KafkaConfiguration {

    private final KafkaProperties kafkaProperties;

    /**
     * Создает ProducerFactory для событий датчиков с Avro сериализацией.
     */
    @Bean
    public ProducerFactory<String, SensorEventAvro> sensorEventProducerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties(null));
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class.getName());
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Создает KafkaTemplate для отправки событий датчиков.
     */
    @Bean
    public KafkaTemplate<String, SensorEventAvro> sensorEventKafkaTemplate() {
        return new KafkaTemplate<>(sensorEventProducerFactory());
    }

    /**
     * Создает ProducerFactory для событий хабов с Avro сериализацией.
     */
    @Bean
    public ProducerFactory<String, HubEventAvro> hubEventProducerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties(null));
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class.getName());
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Создает KafkaTemplate для отправки событий хабов.
     */
    @Bean
    public KafkaTemplate<String, HubEventAvro> hubEventKafkaTemplate() {
        return new KafkaTemplate<>(hubEventProducerFactory());
    }
}