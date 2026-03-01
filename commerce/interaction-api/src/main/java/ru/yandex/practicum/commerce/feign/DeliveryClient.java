package ru.yandex.practicum.commerce.feign;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.commerce.dto.DeliveryDto;
import ru.yandex.practicum.commerce.dto.OrderDto;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "delivery")
public interface DeliveryClient {

    @PutMapping("/api/v1/delivery")
    DeliveryDto planDelivery(@RequestBody @Valid DeliveryDto deliveryDto);

    @PostMapping("/api/v1/delivery/cost")
    BigDecimal deliveryCost(@RequestBody OrderDto orderDto);

    @PostMapping("/api/v1/delivery/picked")
    void deliveryPicked(@RequestBody UUID deliveryId);

    @PostMapping("/api/v1/delivery/successful")
    void deliverySuccessful(@RequestBody UUID deliveryId);

    @PostMapping("/api/v1/delivery/failed")
    void deliveryFailed(@RequestBody UUID deliveryId);
}
