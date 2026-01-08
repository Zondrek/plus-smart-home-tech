package ru.yandex.practicum.telemetry.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.aggregator.configuration.KafkaTopicsProperties;

import java.util.List;
import java.util.Optional;

/**
 * Класс AggregationStarter, ответственный за запуск агрегации данных.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final SnapshotAggregator snapshotAggregator;
    private final KafkaTopicsProperties topicsProperties;
    private final KafkaTemplate<String, SensorsSnapshotAvro> snapshotKafkaTemplate;
    
    @KafkaListener(
            topics = "${kafka.topic.sensors}",
            containerFactory = "sensorEventListenerFactory"
    )
    public void handleSensorEvents(
            @Payload List<ConsumerRecord<String, SensorEventAvro>> records,
            Acknowledgment acknowledgment
    ) {
        if (records.isEmpty()) {
            return;
        }

        log.info("Received {} events from Kafka", records.size());

        // Обрабатываем каждое событие
        for (ConsumerRecord<String, SensorEventAvro> record : records) {
            SensorEventAvro event = record.value();

            // Обновляем состояние снапшота
            Optional<SensorsSnapshotAvro> updatedSnapshot = snapshotAggregator.updateState(event);

            // Если снапшот был обновлен, отправляем его в Kafka
            if (updatedSnapshot.isPresent()) {
                SensorsSnapshotAvro snapshot = updatedSnapshot.get();
                log.info("Sending snapshot for hub {} to Kafka topic {}",
                        snapshot.getHubId(), topicsProperties.getSnapshots());

                try {
                    snapshotKafkaTemplate.send(
                            topicsProperties.getSnapshots(),
                            null,
                            snapshot.getTimestamp().toEpochMilli(),
                            snapshot.getHubId(),
                            snapshot
                    ).get(); // Ждем подтверждения отправки
                } catch (Exception e) {
                    log.error("Error sending snapshot to Kafka", e);
                    throw new RuntimeException("Failed to send snapshot", e);
                }
            }

            // Коммитим офсет после успешной обработки каждого события
            acknowledgment.acknowledge();
        }
    }
}