package ru.yandex.practicum.telemetry.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.analyzer.configuration.KafkaTopicsProperties;
import ru.yandex.practicum.telemetry.analyzer.service.ScenarioAnalyzerService;

import java.time.Duration;
import java.util.Collections;

/**
 * Процессор для обработки снапшотов из Kafka.
 * Читает снапшоты состояния датчиков и анализирует их на соответствие сценариям.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    private final Consumer<String, SensorsSnapshotAvro> snapshotConsumer;
    private final ScenarioAnalyzerService analyzerService;
    private final KafkaTopicsProperties topicsProperties;

    private volatile boolean running = true;

    /**
     * Запускает обработку снапшотов.
     * Должен вызываться в основном потоке приложения.
     */
    public void start() {
        try {
            // Подписываемся на топик
            snapshotConsumer.subscribe(Collections.singletonList(topicsProperties.getSnapshots()));
            log.info("SnapshotProcessor subscribed to topic: {}", topicsProperties.getSnapshots());

            // Цикл опроса
            while (running) {
                try {
                    ConsumerRecords<String, SensorsSnapshotAvro> records =
                            snapshotConsumer.poll(Duration.ofSeconds(1));

                    if (!records.isEmpty()) {
                        log.debug("Received {} snapshots", records.count());

                        records.forEach(record -> {
                            try {
                                analyzerService.analyzeSnapshot(record.value());
                            } catch (Exception e) {
                                log.error("Error analyzing snapshot", e);
                            }
                        });

                        // Фиксируем смещения после успешной обработки
                        snapshotConsumer.commitSync();
                    }
                } catch (Exception e) {
                    log.error("Error in snapshot processing loop", e);
                }
            }
        } finally {
            log.info("SnapshotProcessor shutting down");
            snapshotConsumer.close();
        }
    }

    public void stop() {
        running = false;
    }
}