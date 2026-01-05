package ru.yandex.practicum.telemetry.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.aggregator.configuration.KafkaTopicsProperties;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Класс AggregationStarter, ответственный за запуск агрегации данных.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final Consumer<String, SensorEventAvro> consumer;
    private final Producer<String, SensorsSnapshotAvro> producer;
    private final SnapshotAggregator snapshotAggregator;
    private final KafkaTopicsProperties topicsProperties;

    /**
     * Метод для начала процесса агрегации данных.
     * Подписывается на топики для получения событий от датчиков,
     * формирует снимок их состояния и записывает в Kafka.
     */
    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            // Подписываемся на топик с событиями датчиков
            consumer.subscribe(List.of(topicsProperties.getSensors()));
            log.info("Subscribed to topic: {}", topicsProperties.getSensors());

            // Цикл обработки событий
            while (true) {
                // Получаем события из Kafka
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(1000));

                if (records.isEmpty()) {
                    continue;
                }

                log.info("Received {} events from Kafka", records.count());

                // Обрабатываем каждое событие
                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    SensorEventAvro event = record.value();

                    // Обновляем состояние снапшота
                    Optional<SensorsSnapshotAvro> updatedSnapshot = snapshotAggregator.updateState(event);

                    // Если снапшот был обновлен, отправляем его в Kafka
                    updatedSnapshot.ifPresent(snapshot -> {
                        ProducerRecord<String, SensorsSnapshotAvro> snapshotRecord =
                                new ProducerRecord<>(
                                        topicsProperties.getSnapshots(),
                                        snapshot.getHubId(),
                                        snapshot
                                );

                        producer.send(snapshotRecord, (metadata, exception) -> {
                            if (exception != null) {
                                log.error("Error sending snapshot to Kafka", exception);
                            } else {
                                log.debug("Snapshot sent to topic {}, partition {}, offset {}",
                                        metadata.topic(), metadata.partition(), metadata.offset());
                            }
                        });
                    });
                }

                // Фиксируем смещения после успешной обработки пакета событий
                consumer.commitSync();
                log.debug("Committed offsets for {} records", records.count());
            }

        } catch (WakeupException ignored) {
            // Игнорируем - закрываем консьюмер и продюсер в блоке finally
            log.info("Wakeup exception received, shutting down");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                // Перед тем, как закрыть продюсер и консьюмер, нужно убедиться,
                // что все сообщения, лежащие в буфере, отправлены и
                // все оффсеты обработанных сообщений зафиксированы

                // Сбрасываем данные из буфера продюсера
                log.info("Flushing producer buffer");
                producer.flush();

                // Фиксируем смещения в консьюмере
                log.info("Committing final offsets");
                consumer.commitSync();

            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }
}