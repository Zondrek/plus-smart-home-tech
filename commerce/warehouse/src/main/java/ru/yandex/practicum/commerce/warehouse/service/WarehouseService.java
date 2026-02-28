package ru.yandex.practicum.commerce.warehouse.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.dto.AddProductToWarehouseRequest;
import ru.yandex.practicum.commerce.dto.AddressDto;
import ru.yandex.practicum.commerce.dto.AssemblyProductsForOrderRequest;
import ru.yandex.practicum.commerce.dto.BookedProductsDto;
import ru.yandex.practicum.commerce.dto.NewProductInWarehouseRequest;
import ru.yandex.practicum.commerce.dto.QuantityState;
import ru.yandex.practicum.commerce.dto.ShippedToDeliveryRequest;
import ru.yandex.practicum.commerce.dto.ShoppingCartDto;
import ru.yandex.practicum.commerce.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.commerce.exception.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.commerce.exception.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.commerce.feign.ShoppingStoreClient;
import ru.yandex.practicum.commerce.warehouse.model.OrderBooking;
import ru.yandex.practicum.commerce.warehouse.model.WarehouseProduct;
import ru.yandex.practicum.commerce.warehouse.repository.OrderBookingRepository;
import ru.yandex.practicum.commerce.warehouse.repository.WarehouseProductRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseProductRepository warehouseProductRepository;
    private final OrderBookingRepository orderBookingRepository;
    private final ShoppingStoreClient shoppingStoreClient;

    private static final AddressDto WAREHOUSE_ADDRESS = AddressDto.builder()
            .country("Россия")
            .city("Москва")
            .street("ул. Складская")
            .house("1")
            .flat("1")
            .build();

    @Transactional
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        log.info("Регистрация нового товара на складе: id={}", request.getProductId());
        if (warehouseProductRepository.existsById(request.getProductId())) {
            throw new SpecifiedProductAlreadyInWarehouseException(
                    "Товар с id=" + request.getProductId() + " уже зарегистрирован на складе");
        }

        WarehouseProduct product = WarehouseProduct.builder()
                .productId(request.getProductId())
                .fragile(request.isFragile())
                .width(request.getDimension().getWidth())
                .height(request.getDimension().getHeight())
                .depth(request.getDimension().getDepth())
                .weight(request.getWeight())
                .quantity(0)
                .build();

        warehouseProductRepository.save(product);
        log.info("Товар id={} зарегистрирован на складе", request.getProductId());
    }

    @Transactional
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        log.info("Добавление {} ед. товара id={} на склад", request.getQuantity(), request.getProductId());
        WarehouseProduct product = warehouseProductRepository.findById(request.getProductId())
                .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                        "Товар с id=" + request.getProductId() + " не найден на складе"));

        product.setQuantity(product.getQuantity() + request.getQuantity());
        warehouseProductRepository.save(product);

        QuantityState newState = calculateQuantityState(product.getQuantity());
        log.info("Новое количество товара id={}: {}, состояние: {}", request.getProductId(), product.getQuantity(), newState);
        try {
            shoppingStoreClient.setProductQuantityState(request.getProductId(), newState);
        } catch (FeignException e) {
            log.error("Не удалось обновить quantityState товара id={} в витрине: {}",
                    request.getProductId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto shoppingCart) {
        log.info("Проверка наличия товаров для корзины id={}", shoppingCart.getShoppingCartId());
        BookedProductsDto result = calculateBookingCharacteristics(shoppingCart.getProducts());
        log.info("Бронирование: вес={}, объём={}, хрупкое={}",
                result.getDeliveryWeight(), result.getDeliveryVolume(), result.isFragile());
        return result;
    }

    public AddressDto getWarehouseAddress() {
        log.info("Запрос адреса склада");
        return WAREHOUSE_ADDRESS;
    }

    @Transactional
    public BookedProductsDto assemblyProductsForOrder(AssemblyProductsForOrderRequest request) {
        log.info("Сборка товаров для заказа id={}", request.getOrderId());

        Map<UUID, Long> requestedProducts = request.getProducts();
        BookedProductsDto result = calculateBookingCharacteristics(requestedProducts);

        Map<UUID, WarehouseProduct> productsMap = warehouseProductRepository
                .findAllById(List.copyOf(requestedProducts.keySet()))
                .stream()
                .collect(Collectors.toMap(WarehouseProduct::getProductId, Function.identity()));

        for (Map.Entry<UUID, Long> entry : requestedProducts.entrySet()) {
            UUID productId = entry.getKey();
            long quantity = entry.getValue();
            WarehouseProduct product = productsMap.get(productId);

            product.setQuantity(product.getQuantity() - quantity);
            warehouseProductRepository.save(product);

            QuantityState newState = calculateQuantityState(product.getQuantity());
            try {
                shoppingStoreClient.setProductQuantityState(productId, newState);
            } catch (FeignException e) {
                log.error("Не удалось обновить quantityState товара id={}: {}", productId, e.getMessage());
            }
        }

        OrderBooking booking = OrderBooking.builder()
                .orderId(request.getOrderId())
                .products(new java.util.HashMap<>(requestedProducts))
                .build();
        orderBookingRepository.save(booking);

        log.info("Сборка заказа id={} завершена: вес={}, объём={}, хрупкое={}",
                request.getOrderId(), result.getDeliveryWeight(), result.getDeliveryVolume(), result.isFragile());
        return result;
    }

    @Transactional
    public void shippedToDelivery(ShippedToDeliveryRequest request) {
        log.info("Передача заказа id={} в доставку, deliveryId={}", request.getOrderId(), request.getDeliveryId());
        OrderBooking booking = orderBookingRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                        "Бронирование для заказа id=" + request.getOrderId() + " не найдено"));
        booking.setDeliveryId(request.getDeliveryId());
        orderBookingRepository.save(booking);
        log.info("DeliveryId установлен для заказа id={}", request.getOrderId());
    }

    @Transactional
    public void acceptReturn(Map<UUID, Long> products) {
        log.info("Возврат товаров на склад, количество позиций: {}", products.size());
        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            long quantity = entry.getValue();

            WarehouseProduct product = warehouseProductRepository.findById(productId)
                    .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                            "Товар с id=" + productId + " не найден на складе"));

            product.setQuantity(product.getQuantity() + quantity);
            warehouseProductRepository.save(product);

            QuantityState newState = calculateQuantityState(product.getQuantity());
            try {
                shoppingStoreClient.setProductQuantityState(productId, newState);
            } catch (FeignException e) {
                log.error("Не удалось обновить quantityState товара id={}: {}", productId, e.getMessage());
            }
        }
        log.info("Возврат товаров на склад завершён");
    }

    private BookedProductsDto calculateBookingCharacteristics(Map<UUID, Long> products) {
        List<UUID> productIds = List.copyOf(products.keySet());
        Map<UUID, WarehouseProduct> productsMap = warehouseProductRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(WarehouseProduct::getProductId, Function.identity()));

        double totalWeight = 0;
        double totalVolume = 0;
        boolean fragile = false;

        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            long requestedQuantity = entry.getValue();

            WarehouseProduct product = productsMap.get(productId);
            if (product == null) {
                throw new NoSpecifiedProductInWarehouseException(
                        "Товар с id=" + productId + " не найден на складе");
            }

            if (product.getQuantity() < requestedQuantity) {
                throw new ProductInShoppingCartLowQuantityInWarehouse(
                        "Недостаточно товара с id=" + productId + " на складе");
            }

            totalWeight += product.getWeight() * requestedQuantity;
            totalVolume += product.getWidth() * product.getHeight() * product.getDepth() * requestedQuantity;
            if (product.isFragile()) {
                fragile = true;
            }
        }

        return BookedProductsDto.builder()
                .deliveryWeight(totalWeight)
                .deliveryVolume(totalVolume)
                .fragile(fragile)
                .build();
    }

    private QuantityState calculateQuantityState(long quantity) {
        if (quantity == 0) {
            return QuantityState.ENDED;
        } else if (quantity < 10) {
            return QuantityState.FEW;
        } else if (quantity < 100) {
            return QuantityState.ENOUGH;
        } else {
            return QuantityState.MANY;
        }
    }
}
