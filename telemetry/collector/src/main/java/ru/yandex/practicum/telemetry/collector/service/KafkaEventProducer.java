package ru.yandex.practicum.telemetry.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.configuration.KafkaTopicsProperties;

/**
 * Сервис для отправки событий в Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, SensorEventAvro> sensorEventKafkaTemplate;
    private final KafkaTemplate<String, HubEventAvro> hubEventKafkaTemplate;
    private final KafkaTopicsProperties topicsProperties;

    /**
     * Отправляет событие датчика в Kafka асинхронно.
     */
    public void sendSensorEvent(SensorEventAvro event) {
        log.info("Sending sensor event to Kafka: hubId={}, sensorId={}",
                event.getHubId(), event.getId());

        sensorEventKafkaTemplate.send(
                topicsProperties.getSensorsEvents(),
                event.getHubId(),
                event
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("Failed to send sensor event to Kafka: id={}, hubId={}",
                        event.getId(), event.getHubId(), exception);
            } else {
                log.debug("Sensor event sent successfully: partition={}, offset={}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });

        sensorEventKafkaTemplate.flush();
    }

    /**
     * Отправляет событие хаба в Kafka асинхронно.
     */
    public void sendHubEvent(HubEventAvro event) {
        log.info("Sending hub event to Kafka: hubId={}", event.getHubId());

        hubEventKafkaTemplate.send(
                topicsProperties.getHubsEvents(),
                event.getHubId(),
                event
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("Failed to send hub event to Kafka: hubId={}",
                        event.getHubId(), exception);
            } else {
                log.debug("Hub event sent successfully: partition={}, offset={}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });

        hubEventKafkaTemplate.flush();
    }
}