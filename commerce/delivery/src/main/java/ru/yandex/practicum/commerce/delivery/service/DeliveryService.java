package ru.yandex.practicum.commerce.delivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.delivery.mapper.DeliveryMapper;
import ru.yandex.practicum.commerce.delivery.model.Delivery;
import ru.yandex.practicum.commerce.delivery.repository.DeliveryRepository;
import ru.yandex.practicum.commerce.dto.DeliveryDto;
import ru.yandex.practicum.commerce.dto.DeliveryState;
import ru.yandex.practicum.commerce.dto.OrderDto;
import ru.yandex.practicum.commerce.dto.ShippedToDeliveryRequest;
import ru.yandex.practicum.commerce.exception.NoDeliveryFoundException;
import ru.yandex.practicum.commerce.feign.OrderClient;
import ru.yandex.practicum.commerce.feign.WarehouseClient;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryMapper deliveryMapper;
    private final OrderClient orderClient;
    private final WarehouseClient warehouseClient;

    private static final BigDecimal BASE_COST = BigDecimal.valueOf(5.0);
    private static final String ADDRESS_2 = "ADDRESS_2";

    @Transactional
    public DeliveryDto planDelivery(DeliveryDto deliveryDto) {
        log.info("Планирование доставки для заказа id={}", deliveryDto.getOrderId());
        Delivery delivery = deliveryMapper.toEntity(deliveryDto);
        delivery.setDeliveryState(DeliveryState.CREATED);
        Delivery saved = deliveryRepository.save(delivery);
        log.info("Доставка id={} создана для заказа id={}", saved.getDeliveryId(), saved.getOrderId());
        return deliveryMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public BigDecimal deliveryCost(OrderDto orderDto) {
        log.info("Расчёт стоимости доставки для заказа id={}", orderDto.getOrderId());
        Delivery delivery = deliveryRepository.findById(orderDto.getDeliveryId())
                .orElseThrow(() -> new NoDeliveryFoundException(
                        "Доставка с id=" + orderDto.getDeliveryId() + " не найдена"));

        BigDecimal cost = BASE_COST;

        // Множитель адреса + сложение с базовой стоимостью
        String fromStreet = delivery.getFromStreet();
        if (ADDRESS_2.equals(fromStreet)) {
            cost = cost.multiply(BigDecimal.valueOf(2));
        }
        cost = cost.add(BASE_COST);

        // Хрупкость
        if (orderDto.isFragile()) {
            cost = cost.add(cost.multiply(BigDecimal.valueOf(0.2)));
        }

        // Вес
        cost = cost.add(BigDecimal.valueOf(orderDto.getDeliveryWeight()).multiply(BigDecimal.valueOf(0.3)));

        // Объём
        cost = cost.add(BigDecimal.valueOf(orderDto.getDeliveryVolume()).multiply(BigDecimal.valueOf(0.2)));

        // Несовпадение улиц (наценка)
        if (delivery.getFromStreet() == null || !delivery.getFromStreet().equals(delivery.getToStreet())) {
            cost = cost.add(cost.multiply(BigDecimal.valueOf(0.2)));
        }

        log.info("Стоимость доставки для заказа id={}: {}", orderDto.getOrderId(), cost);
        return cost;
    }

    @Transactional
    public void deliveryPicked(UUID deliveryId) {
        log.info("Доставка id={} забрана", deliveryId);
        Delivery delivery = findDeliveryById(deliveryId);
        delivery.setDeliveryState(DeliveryState.IN_PROGRESS);
        deliveryRepository.save(delivery);

        orderClient.assembly(delivery.getOrderId());
        warehouseClient.shippedToDelivery(ShippedToDeliveryRequest.builder()
                .orderId(delivery.getOrderId())
                .deliveryId(deliveryId)
                .build());
        log.info("Доставка id={} в процессе", deliveryId);
    }

    @Transactional
    public void deliverySuccessful(UUID deliveryId) {
        log.info("Доставка id={} успешна", deliveryId);
        Delivery delivery = findDeliveryById(deliveryId);
        delivery.setDeliveryState(DeliveryState.DELIVERED);
        deliveryRepository.save(delivery);
        orderClient.delivery(delivery.getOrderId());
        log.info("Доставка id={} завершена", deliveryId);
    }

    @Transactional
    public void deliveryFailed(UUID deliveryId) {
        log.info("Ошибка доставки id={}", deliveryId);
        Delivery delivery = findDeliveryById(deliveryId);
        delivery.setDeliveryState(DeliveryState.FAILED);
        deliveryRepository.save(delivery);
        orderClient.deliveryFailed(delivery.getOrderId());
        log.info("Доставка id={} не выполнена", deliveryId);
    }

    private Delivery findDeliveryById(UUID deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoDeliveryFoundException("Доставка с id=" + deliveryId + " не найдена"));
    }
}
