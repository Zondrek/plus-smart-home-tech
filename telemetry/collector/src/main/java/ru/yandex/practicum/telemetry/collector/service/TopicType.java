package ru.yandex.practicum.telemetry.collector.service;

import lombok.Getter;

@Getter
public enum TopicType {
    SENSORS_EVENTS("sensors-events"),
    HUBS_EVENTS("hubs-events");

    private final String key;

    TopicType(String key) {
        this.key = key;
    }

}