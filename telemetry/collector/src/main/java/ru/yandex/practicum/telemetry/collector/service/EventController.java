package ru.yandex.practicum.telemetry.collector.service;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.kafka.core.KafkaTemplate;
import ru.yandex.practicum.grpc.telemetry.service.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.configuration.KafkaTopicsProperties;
import ru.yandex.practicum.telemetry.collector.mapper.HubEventMapper;
import ru.yandex.practicum.telemetry.collector.mapper.SensorEventMapper;

import java.util.concurrent.TimeUnit;

/**
 * gRPC сервис для сбора событий от датчиков и хабов.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class EventController extends CollectorControllerGrpc.CollectorControllerImplBase {

    private final KafkaTemplate<String, SensorEventAvro> sensorEventKafkaTemplate;
    private final KafkaTemplate<String, HubEventAvro> hubEventKafkaTemplate;
    private final SensorEventMapper sensorEventMapper;
    private final HubEventMapper hubEventMapper;
    private final KafkaTopicsProperties topicsProperties;

    /**
     * Обрабатывает событие от датчика.
     */
    @Override
    public void collectSensorEvent(SensorEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Received sensor event: type={}, id={}, hubId={}",
                    request.getPayloadCase(), request.getId(), request.getHubId());

            // Преобразуем proto в avro
            SensorEventAvro avroEvent = sensorEventMapper.mapToAvro(request);

            // Отправляем в Kafka синхронно с timeout
            sensorEventKafkaTemplate.send(
                    topicsProperties.getSensors(),
                    avroEvent.getHubId(),
                    avroEvent
            ).get(3, TimeUnit.SECONDS);

            log.debug("Sensor event sent to Kafka successfully");

            // Возвращаем успешный ответ
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing sensor event", e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }

    /**
     * Обрабатывает событие от хаба.
     */
    @Override
    public void collectHubEvent(HubEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Received hub event: type={}, hubId={}",
                    request.getPayloadCase(), request.getHubId());

            // Преобразуем proto в avro
            HubEventAvro avroEvent = hubEventMapper.mapToAvro(request);

            // Отправляем в Kafka синхронно с timeout
            hubEventKafkaTemplate.send(
                    topicsProperties.getHubs(),
                    avroEvent.getHubId(),
                    avroEvent
            ).get(3, TimeUnit.SECONDS);

            log.debug("Hub event sent to Kafka successfully");

            // Возвращаем успешный ответ
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing hub event", e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}