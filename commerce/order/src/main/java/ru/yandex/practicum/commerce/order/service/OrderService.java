package ru.yandex.practicum.commerce.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.dto.AssemblyProductsForOrderRequest;
import ru.yandex.practicum.commerce.dto.BookedProductsDto;
import ru.yandex.practicum.commerce.dto.CreateNewOrderRequest;
import ru.yandex.practicum.commerce.dto.DeliveryDto;
import ru.yandex.practicum.commerce.dto.OrderDto;
import ru.yandex.practicum.commerce.dto.OrderState;
import ru.yandex.practicum.commerce.dto.PaymentDto;
import ru.yandex.practicum.commerce.dto.ProductReturnRequest;
import ru.yandex.practicum.commerce.exception.NoOrderFoundException;
import ru.yandex.practicum.commerce.feign.DeliveryClient;
import ru.yandex.practicum.commerce.feign.PaymentClient;
import ru.yandex.practicum.commerce.feign.WarehouseClient;
import ru.yandex.practicum.commerce.order.mapper.OrderMapper;
import ru.yandex.practicum.commerce.order.model.Order;
import ru.yandex.practicum.commerce.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final DeliveryClient deliveryClient;
    private final PaymentClient paymentClient;
    private final WarehouseClient warehouseClient;

    @Transactional(readOnly = true)
    public List<OrderDto> getClientOrders(String username) {
        log.info("Получение заказов пользователя: {}", username);
        return orderRepository.findAllByUsername(username)
                .stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDto createNewOrder(CreateNewOrderRequest request) {
        log.info("Создание нового заказа для корзины id={}", request.getShoppingCart().getShoppingCartId());

        Order order = Order.builder()
                .shoppingCartId(request.getShoppingCart().getShoppingCartId())
                .state(OrderState.NEW)
                .products(new HashMap<>(request.getShoppingCart().getProducts()))
                .build();

        Order saved = orderRepository.save(order);

        warehouseClient.checkProductQuantityEnoughForShoppingCart(request.getShoppingCart());

        DeliveryDto deliveryDto = deliveryClient.planDelivery(DeliveryDto.builder()
                .fromAddress(warehouseClient.getWarehouseAddress())
                .toAddress(request.getDeliveryAddress())
                .orderId(saved.getOrderId())
                .build());
        saved.setDeliveryId(deliveryDto.getDeliveryId());
        orderRepository.save(saved);

        log.info("Заказ id={} создан", saved.getOrderId());
        return orderMapper.toDto(saved);
    }

    @Transactional
    public OrderDto productReturn(ProductReturnRequest request) {
        log.info("Возврат товаров по заказу id={}", request.getOrderId());
        Order order = findOrderById(request.getOrderId());
        order.setState(OrderState.PRODUCT_RETURNED);
        orderRepository.save(order);
        warehouseClient.acceptReturn(request.getProducts());
        log.info("Заказ id={} — товары возвращены", request.getOrderId());
        return orderMapper.toDto(order);
    }

    @Transactional
    public OrderDto payment(UUID orderId) {
        log.info("Оплата заказа id={}", orderId);
        Order order = findOrderById(orderId);

        if (order.getPaymentId() != null) {
            order.setState(OrderState.PAID);
            orderRepository.save(order);
            log.info("Заказ id={} оплачен", orderId);
            return orderMapper.toDto(order);
        }

        OrderDto orderDto = orderMapper.toDto(order);
        PaymentDto paymentDto = paymentClient.payment(orderDto);
        order.setPaymentId(paymentDto.getPaymentId());
        order.setState(OrderState.ON_PAYMENT);
        orderRepository.save(order);
        log.info("Заказ id={} отправлен на оплату, paymentId={}", orderId, paymentDto.getPaymentId());
        return orderMapper.toDto(order);
    }

    @Transactional
    public OrderDto paymentFailed(UUID orderId) {
        log.info("Ошибка оплаты заказа id={}", orderId);
        Order order = findOrderById(orderId);
        order.setState(OrderState.PAYMENT_FAILED);
        orderRepository.save(order);
        return orderMapper.toDto(order);
    }

    @Transactional
    public OrderDto delivery(UUID orderId) {
        log.info("Заказ id={} доставлен", orderId);
        Order order = findOrderById(orderId);
        order.setState(OrderState.DELIVERED);
        orderRepository.save(order);
        return orderMapper.toDto(order);
    }

    @Transactional
    public OrderDto deliveryFailed(UUID orderId) {
        log.info("Ошибка доставки заказа id={}", orderId);
        Order order = findOrderById(orderId);
        order.setState(OrderState.DELIVERY_FAILED);
        orderRepository.save(order);
        return orderMapper.toDto(order);
    }

    @Transactional
    public OrderDto complete(UUID orderId) {
        log.info("Завершение заказа id={}", orderId);
        Order order = findOrderById(orderId);
        order.setState(OrderState.COMPLETED);
        orderRepository.save(order);
        return orderMapper.toDto(order);
    }

    @Transactional
    public OrderDto calculateTotalCost(UUID orderId) {
        log.info("Расчёт полной стоимости заказа id={}", orderId);
        Order order = findOrderById(orderId);
        OrderDto orderDto = orderMapper.toDto(order);

        BigDecimal productCost = paymentClient.productCost(orderDto);
        order.setProductPrice(productCost);
        orderRepository.save(order);

        orderDto = orderMapper.toDto(order);
        BigDecimal totalCost = paymentClient.getTotalCost(orderDto);
        order.setTotalPrice(totalCost);
        orderRepository.save(order);
        log.info("Полная стоимость заказа id={}: {}", orderId, totalCost);
        return orderMapper.toDto(order);
    }

    @Transactional
    public OrderDto calculateDeliveryCost(UUID orderId) {
        log.info("Расчёт стоимости доставки заказа id={}", orderId);
        Order order = findOrderById(orderId);
        OrderDto orderDto = orderMapper.toDto(order);
        BigDecimal deliveryCost = deliveryClient.deliveryCost(orderDto);
        order.setDeliveryPrice(deliveryCost);
        orderRepository.save(order);
        log.info("Стоимость доставки заказа id={}: {}", orderId, deliveryCost);
        return orderMapper.toDto(order);
    }

    @Transactional
    public OrderDto assembly(UUID orderId) {
        log.info("Сборка заказа id={}", orderId);
        Order order = findOrderById(orderId);

        AssemblyProductsForOrderRequest assemblyRequest = AssemblyProductsForOrderRequest.builder()
                .orderId(orderId)
                .products(order.getProducts())
                .build();
        BookedProductsDto booked = warehouseClient.assemblyProductsForOrder(assemblyRequest);

        order.setDeliveryWeight(booked.getDeliveryWeight());
        order.setDeliveryVolume(booked.getDeliveryVolume());
        order.setFragile(booked.isFragile());
        order.setState(OrderState.ASSEMBLED);
        orderRepository.save(order);

        log.info("Заказ id={} собран", orderId);
        return orderMapper.toDto(order);
    }

    @Transactional
    public OrderDto assemblyFailed(UUID orderId) {
        log.info("Ошибка сборки заказа id={}", orderId);
        Order order = findOrderById(orderId);
        order.setState(OrderState.ASSEMBLY_FAILED);
        orderRepository.save(order);
        return orderMapper.toDto(order);
    }

    private Order findOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Заказ с id=" + orderId + " не найден"));
    }
}
