package ru.yandex.practicum.telemetry.collector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Главный класс сервиса Collector.
 * Сервис принимает события от датчиков и хабов через gRPC
 * и записывает их в Kafka.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class Collector {

    public static void main(String[] args) {
        SpringApplication.run(Collector.class, args);
    }
}