package ru.yandex.practicum.commerce.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.dto.OrderDto;
import ru.yandex.practicum.commerce.dto.PaymentDto;
import ru.yandex.practicum.commerce.dto.PaymentState;
import ru.yandex.practicum.commerce.dto.ProductDto;
import ru.yandex.practicum.commerce.exception.NoPaymentFoundException;
import ru.yandex.practicum.commerce.feign.OrderClient;
import ru.yandex.practicum.commerce.feign.ShoppingStoreClient;
import ru.yandex.practicum.commerce.payment.mapper.PaymentMapper;
import ru.yandex.practicum.commerce.payment.model.Payment;
import ru.yandex.practicum.commerce.payment.repository.PaymentRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final ShoppingStoreClient shoppingStoreClient;
    private final OrderClient orderClient;

    private static final double VAT_RATE = 0.1;

    public Double productCost(OrderDto orderDto) {
        log.info("Расчёт стоимости товаров для заказа id={}", orderDto.getOrderId());
        List<UUID> productIds = List.copyOf(orderDto.getProducts().keySet());
        Map<UUID, ProductDto> productsMap = shoppingStoreClient.getProductsByIds(productIds)
                .stream()
                .collect(Collectors.toMap(ProductDto::getProductId, Function.identity()));

        double totalProductCost = 0;
        for (Map.Entry<UUID, Long> entry : orderDto.getProducts().entrySet()) {
            ProductDto product = productsMap.get(entry.getKey());
            if (product != null) {
                totalProductCost += product.getPrice() * entry.getValue();
            }
        }
        log.info("Стоимость товаров для заказа id={}: {}", orderDto.getOrderId(), totalProductCost);
        return totalProductCost;
    }

    public Double getTotalCost(OrderDto orderDto) {
        log.info("Расчёт полной стоимости для заказа id={}", orderDto.getOrderId());
        double productPrice = orderDto.getProductPrice();
        double fee = productPrice * VAT_RATE;
        double total = productPrice + fee + orderDto.getDeliveryPrice();
        log.info("Полная стоимость заказа id={}: {} (товары={}, НДС={}, доставка={})",
                orderDto.getOrderId(), total, productPrice, fee, orderDto.getDeliveryPrice());
        return total;
    }

    @Transactional
    public PaymentDto payment(OrderDto orderDto) {
        log.info("Создание платежа для заказа id={}", orderDto.getOrderId());
        double productTotal = orderDto.getProductPrice();
        double feeTotal = productTotal * VAT_RATE;
        double deliveryTotal = orderDto.getDeliveryPrice();
        double totalPayment = productTotal + feeTotal + deliveryTotal;

        Payment payment = Payment.builder()
                .orderId(orderDto.getOrderId())
                .productTotal(productTotal)
                .feeTotal(feeTotal)
                .deliveryTotal(deliveryTotal)
                .totalPayment(totalPayment)
                .status(PaymentState.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Платёж id={} создан для заказа id={}", saved.getPaymentId(), orderDto.getOrderId());

        return paymentMapper.toDto(saved);
    }

    @Transactional
    public void paymentSuccess(UUID paymentId) {
        log.info("Успешная оплата paymentId={}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoPaymentFoundException("Платёж с id=" + paymentId + " не найден"));
        payment.setStatus(PaymentState.SUCCESS);
        paymentRepository.save(payment);
        orderClient.payment(payment.getOrderId());
        log.info("Платёж id={} помечен как SUCCESS", paymentId);
    }

    @Transactional
    public void paymentFailed(UUID paymentId) {
        log.info("Ошибка оплаты paymentId={}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoPaymentFoundException("Платёж с id=" + paymentId + " не найден"));
        payment.setStatus(PaymentState.FAILED);
        paymentRepository.save(payment);
        orderClient.paymentFailed(payment.getOrderId());
        log.info("Платёж id={} помечен как FAILED", paymentId);
    }
}
