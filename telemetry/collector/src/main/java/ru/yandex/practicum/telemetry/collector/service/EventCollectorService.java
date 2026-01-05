package ru.yandex.practicum.telemetry.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.service.mapper.HubEventMapper;
import ru.yandex.practicum.telemetry.collector.service.mapper.SensorEventMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventCollectorService {

    private final SensorEventMapper sensorEventMapper;
    private final HubEventMapper hubEventMapper;
    private final KafkaEventProducer kafkaEventProducer;

    public void collectSensorEvent(SensorEvent event) {
        log.debug("Collecting sensor event: {}", event);
        SensorEventAvro avroEvent = sensorEventMapper.toAvro(event);
        kafkaEventProducer.sendSensorEvent(avroEvent);
    }

    public void collectHubEvent(HubEvent event) {
        log.debug("Collecting hub event: {}", event);
        HubEventAvro avroEvent = hubEventMapper.toAvro(event);
        kafkaEventProducer.sendHubEvent(avroEvent);
    }
}