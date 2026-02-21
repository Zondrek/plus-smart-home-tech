package ru.yandex.practicum.telemetry.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Главный класс сервиса Aggregator.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableKafka
public class Aggregator {

    public static void main(String[] args) {
        SpringApplication.run(Aggregator.class, args);
    }
}