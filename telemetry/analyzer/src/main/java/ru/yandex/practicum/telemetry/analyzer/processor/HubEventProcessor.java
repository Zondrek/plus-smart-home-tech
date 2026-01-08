package ru.yandex.practicum.telemetry.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.analyzer.service.HubEventService;

import java.util.List;

/**
 * Процессор для обработки событий хабов из Kafka..
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor {

    private final HubEventService hubEventService;

    @KafkaListener(
            topics = "${kafka.topic.hubs}",
            groupId = "analyzer-hub-events-group",
            containerFactory = "hubEventListenerFactory"
    )
    public void handleHubEvents(List<HubEventAvro> events, Acknowledgment acknowledgment) {
        if (events.isEmpty()) {
            return;
        }

        log.debug("Received {} hub events", events.size());

        for (HubEventAvro event : events) {
            hubEventService.handleHubEvent(event);
            acknowledgment.acknowledge();
        }
    }
}