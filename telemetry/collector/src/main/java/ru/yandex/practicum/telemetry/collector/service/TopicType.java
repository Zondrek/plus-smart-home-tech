package ru.yandex.practicum.telemetry.collector.service;

public enum TopicType {
    SENSORS_EVENTS("sensors-events"),
    HUBS_EVENTS("hubs-events");

    private final String key;

    TopicType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}