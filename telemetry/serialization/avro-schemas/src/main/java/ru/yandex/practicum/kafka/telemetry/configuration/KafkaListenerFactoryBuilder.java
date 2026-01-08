package ru.yandex.practicum.kafka.telemetry.configuration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;


public class KafkaListenerFactoryBuilder {

    /**
     * Создает ConcurrentKafkaListenerContainerFactory с указанным deserializer'ом.
     *
     * @param kafkaProperties Spring Boot Kafka properties
     * @param deserializerClass класс deserializer'а для value
     * @param autoOffsetReset стратегия auto.offset.reset (может быть null)
     * @param <T> тип событий Avro
     * @return настроенный ContainerFactory
     */
    public static <T> ConcurrentKafkaListenerContainerFactory<String, T> create(
            KafkaProperties kafkaProperties,
            Class<?> deserializerClass,
            String autoOffsetReset) {

        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializerClass.getName());

        if (autoOffsetReset != null) {
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        }

        ConsumerFactory<String, T> factory = new DefaultKafkaConsumerFactory<>(props);
        ConcurrentKafkaListenerContainerFactory<String, T> containerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(factory);
        containerFactory.setBatchListener(true);
        containerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return containerFactory;
    }
    
    public static <T> ConcurrentKafkaListenerContainerFactory<String, T> create(
            KafkaProperties kafkaProperties,
            Class<?> deserializerClass) {
        return create(kafkaProperties, deserializerClass, null);
    }
}