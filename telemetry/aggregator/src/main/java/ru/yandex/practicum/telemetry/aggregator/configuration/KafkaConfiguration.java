package ru.yandex.practicum.telemetry.aggregator.configuration;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.aggregator.configuration.serialization.AvroSerializer;
import ru.yandex.practicum.telemetry.aggregator.configuration.serialization.SensorEventDeserializer;

import java.util.Properties;

/**
 * Конфигурация Kafka для сервиса Aggregator.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
@RequiredArgsConstructor
public class KafkaConfiguration {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Создает консьюмер для чтения событий датчиков из Kafka.
     *
     * @return экземпляр Consumer для SensorEventAvro
     */
    @Bean
    public Consumer<String, SensorEventAvro> sensorEventConsumer() {
        Properties props = new Properties();

        // Берем базовые настройки из Spring Kafka
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaTemplate.getProducerFactory().getConfigurationProperties()
                        .get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));

        props.put(ConsumerConfig.GROUP_ID_CONFIG, "aggregator-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                SensorEventDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        return new KafkaConsumer<>(props);
    }

    /**
     * Создает продюсер для отправки снапшотов в Kafka.
     *
     * @return экземпляр Producer для SensorsSnapshotAvro
     */
    @Bean
    public Producer<String, SensorsSnapshotAvro> snapshotProducer() {
        Properties props = new Properties();

        // Берем базовые настройки из Spring Kafka
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaTemplate.getProducerFactory().getConfigurationProperties()
                        .get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                AvroSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        return new KafkaProducer<>(props);
    }
}