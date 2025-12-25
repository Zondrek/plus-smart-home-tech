package ru.yandex.practicum.telemetry.collector.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.configuration.KafkaConfigurationProperties;

import java.util.Map;

@Slf4j
@Service
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<String, String> topics;

    public KafkaEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
                              KafkaConfigurationProperties kafkaConfig) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = kafkaConfig.getTopics();
    }

    public void sendSensorEvent(SensorEventAvro event) {
        String topic = topics.get(TopicType.SENSORS_EVENTS.getKey());
        String key = event.getHubId();
        Long timestamp = event.getTimestamp().toEpochMilli();

        log.info("Sending sensor event to Kafka topic {}: hubId={}, sensorId={}, timestamp={}",
                topic, event.getHubId(), event.getId(), timestamp);

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, null, timestamp, key, event);

        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Successfully sent sensor event: hubId={}, sensorId={}, offset={}",
                        event.getHubId(), event.getId(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send sensor event: hubId={}, sensorId={}, error={}",
                        event.getHubId(), event.getId(), ex.getMessage(), ex);
            }
        });
    }

    public void sendHubEvent(HubEventAvro event) {
        String topic = topics.get(TopicType.HUBS_EVENTS.getKey());
        String key = event.getHubId();
        Long timestamp = event.getTimestamp().toEpochMilli();

        log.info("Sending hub event to Kafka topic {}: hubId={}, timestamp={}",
                topic, event.getHubId(), timestamp);

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, null, timestamp, key, event);

        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Successfully sent hub event: hubId={}, offset={}",
                        event.getHubId(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send hub event: hubId={}, error={}",
                        event.getHubId(), ex.getMessage(), ex);
            }
        });
    }
}