package ru.yandex.practicum.telemetry.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.analyzer.service.ScenarioAnalyzerService;

import java.util.List;

/**
 * Процессор для обработки снапшотов из Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    private final ScenarioAnalyzerService analyzerService;
    
    @KafkaListener(
            topics = "${kafka.topic.snapshots}",
            groupId = "analyzer-snapshots-group",
            containerFactory = "snapshotListenerFactory"
    )
    public void handleSnapshots(List<SensorsSnapshotAvro> snapshots, Acknowledgment acknowledgment) {
        if (snapshots.isEmpty()) {
            return;
        }

        log.debug("Received {} snapshots", snapshots.size());

        for (SensorsSnapshotAvro snapshot : snapshots) {
            analyzerService.analyzeSnapshot(snapshot);
            acknowledgment.acknowledge();
        }
    }
}