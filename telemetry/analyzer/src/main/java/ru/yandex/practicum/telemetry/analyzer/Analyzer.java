package ru.yandex.practicum.telemetry.analyzer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.telemetry.analyzer.processor.HubEventProcessor;
import ru.yandex.practicum.telemetry.analyzer.processor.SnapshotProcessor;

/**
 * Главный класс приложения Analyzer.
 * Запускает два процессора в разных потоках:
 * - HubEventProcessor - обрабатывает события хабов (добавление/удаление устройств и сценариев)
 * - SnapshotProcessor - обрабатывает снапшоты состояния датчиков и выполняет сценарии
 */
@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class Analyzer {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Analyzer.class, args);

        final HubEventProcessor hubEventProcessor = context.getBean(HubEventProcessor.class);
        final SnapshotProcessor snapshotProcessor = context.getBean(SnapshotProcessor.class);

        // Регистрируем хук для корректного завершения
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown signal received, stopping processors...");
            hubEventProcessor.stop();
            snapshotProcessor.stop();
        }));

        // Запускаем в отдельном потоке обработчик событий от пользовательских хабов
        Thread hubEventsThread = new Thread(hubEventProcessor);
        hubEventsThread.setName("HubEventHandlerThread");
        hubEventsThread.start();

        log.info("HubEventProcessor started in separate thread");

        // В текущем потоке начинаем обработку снимков состояния датчиков
        log.info("Starting SnapshotProcessor in main thread");
        snapshotProcessor.start();
    }
}