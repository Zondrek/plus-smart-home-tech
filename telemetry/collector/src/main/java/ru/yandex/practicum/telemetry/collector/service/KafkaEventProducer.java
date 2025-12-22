package ru.yandex.practicum.telemetry.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${collector.kafka.topic.sensors}")
    private String sensorsTopic;

    @Value("${collector.kafka.topic.hubs}")
    private String hubsTopic;

    public void sendSensorEvent(SensorEventAvro event) {
        log.info("Sending sensor event to Kafka topic {}: hubId={}, sensorId={}",
                sensorsTopic, event.getHubId(), event.getId());
        kafkaTemplate.send(sensorsTopic, event.getHubId().toString(), event);
    }

    public void sendHubEvent(HubEventAvro event) {
        log.info("Sending hub event to Kafka topic {}: hubId={}",
                hubsTopic, event.getHubId());
        kafkaTemplate.send(hubsTopic, event.getHubId().toString(), event);
    }
}