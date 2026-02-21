package ru.yandex.practicum.telemetry.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Главный класс приложения Analyzer.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableKafka
public class Analyzer {

    public static void main(String[] args) {
        SpringApplication.run(Analyzer.class, args);
    }
}