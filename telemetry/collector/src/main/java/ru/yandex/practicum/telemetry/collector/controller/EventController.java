package ru.yandex.practicum.telemetry.collector.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.service.EventCollectorService;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventCollectorService eventCollectorService;

    @PostMapping("/sensors")
    public void collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        log.info("Received sensor event: {}", event);
        eventCollectorService.collectSensorEvent(event);
    }

    @PostMapping("/hubs")
    public void collectHubEvent(@Valid @RequestBody HubEvent event) {
        log.info("Received hub event: {}", event);
        eventCollectorService.collectHubEvent(event);
    }
}