package ru.yandex.practicum.telemetry.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис для агрегации событий датчиков в снапшоты.
 * Хранит текущее состояние всех хабов и их датчиков.
 */
@Slf4j
@Service
public class SnapshotAggregator {

    /**
     * Хранилище снапшотов для всех хабов.
     * Ключ - идентификатор хаба, значение - снапшот состояния датчиков этого хаба.
     */
    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    /**
     * Обновляет состояние датчика в снапшоте.
     * Создает новый снапшот, если для данного хаба его еще не было.
     *
     * @param event событие от датчика
     * @return Optional с обновленным снапшотом, если состояние изменилось, иначе Optional.empty()
     */
    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        // Получаем или создаем снапшот для данного хаба
        SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(
                event.getHubId(),
                hubId -> SensorsSnapshotAvro.newBuilder()
                        .setHubId(hubId)
                        .setTimestamp(event.getTimestamp())
                        .setSensorsState(new HashMap<>())
                        .build()
        );

        // Проверяем, есть ли в снапшоте данные для данного датчика
        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();
        SensorStateAvro oldState = sensorsState.get(event.getId());

        if (oldState != null) {
            // Проверяем, не устарело ли полученное событие
            if (oldState.getTimestamp().compareTo(event.getTimestamp()) >= 0) {
                log.debug("Event for sensor {} is older than current state, ignoring", event.getId());
                return Optional.empty();
            }

            // Проверяем, изменились ли данные
            if (oldState.getData().equals(event.getPayload())) {
                log.debug("Event for sensor {} has same data as current state, ignoring", event.getId());
                return Optional.empty();
            }
        }

        // Создаем новое состояние датчика
        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();

        // Обновляем снапшот
        sensorsState.put(event.getId(), newState);
        snapshot.setTimestamp(event.getTimestamp());

        log.info("Updated snapshot for hub {}, sensor {}", event.getHubId(), event.getId());
        return Optional.of(snapshot);
    }
}