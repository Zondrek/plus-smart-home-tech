package ru.yandex.practicum.telemetry.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.analyzer.configuration.KafkaTopicsProperties;
import ru.yandex.practicum.telemetry.analyzer.service.HubEventService;

import java.time.Duration;
import java.util.Collections;

/**
 * Процессор для обработки событий хабов из Kafka.
 * Работает в отдельном потоке и читает события из топика telemetry.hubs.v1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final Consumer<String, HubEventAvro> hubEventConsumer;
    private final HubEventService hubEventService;
    private final KafkaTopicsProperties topicsProperties;

    private volatile boolean running = true;

    @Override
    public void run() {
        try {
            // Подписываемся на топик
            hubEventConsumer.subscribe(Collections.singletonList(topicsProperties.getHubs()));
            log.info("HubEventProcessor subscribed to topic: {}", topicsProperties.getHubs());

            // Цикл опроса
            while (running) {
                try {
                    ConsumerRecords<String, HubEventAvro> records =
                            hubEventConsumer.poll(Duration.ofSeconds(1));

                    if (!records.isEmpty()) {
                        log.debug("Received {} hub events", records.count());

                        records.forEach(record -> {
                            try {
                                hubEventService.handleHubEvent(record.value());
                            } catch (Exception e) {
                                log.error("Error processing hub event", e);
                            }
                        });

                        // Фиксируем смещения после успешной обработки
                        hubEventConsumer.commitSync();
                    }
                } catch (Exception e) {
                    log.error("Error in hub event processing loop", e);
                }
            }
        } finally {
            log.info("HubEventProcessor shutting down");
            hubEventConsumer.close();
        }
    }

    public void stop() {
        running = false;
    }
}