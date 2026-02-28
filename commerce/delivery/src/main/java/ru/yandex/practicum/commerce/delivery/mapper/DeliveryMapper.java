package ru.yandex.practicum.commerce.delivery.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.commerce.delivery.model.Delivery;
import ru.yandex.practicum.commerce.dto.AddressDto;
import ru.yandex.practicum.commerce.dto.DeliveryDto;

@Component
public class DeliveryMapper {

    public DeliveryDto toDto(Delivery delivery) {
        return DeliveryDto.builder()
                .deliveryId(delivery.getDeliveryId())
                .orderId(delivery.getOrderId())
                .deliveryState(delivery.getDeliveryState())
                .fromAddress(AddressDto.builder()
                        .country(delivery.getFromCountry())
                        .city(delivery.getFromCity())
                        .street(delivery.getFromStreet())
                        .house(delivery.getFromHouse())
                        .flat(delivery.getFromFlat())
                        .build())
                .toAddress(AddressDto.builder()
                        .country(delivery.getToCountry())
                        .city(delivery.getToCity())
                        .street(delivery.getToStreet())
                        .house(delivery.getToHouse())
                        .flat(delivery.getToFlat())
                        .build())
                .build();
    }

    public Delivery toEntity(DeliveryDto dto) {
        Delivery.DeliveryBuilder builder = Delivery.builder()
                .orderId(dto.getOrderId())
                .deliveryState(dto.getDeliveryState());

        if (dto.getFromAddress() != null) {
            builder.fromCountry(dto.getFromAddress().getCountry())
                    .fromCity(dto.getFromAddress().getCity())
                    .fromStreet(dto.getFromAddress().getStreet())
                    .fromHouse(dto.getFromAddress().getHouse())
                    .fromFlat(dto.getFromAddress().getFlat());
        }
        if (dto.getToAddress() != null) {
            builder.toCountry(dto.getToAddress().getCountry())
                    .toCity(dto.getToAddress().getCity())
                    .toStreet(dto.getToAddress().getStreet())
                    .toHouse(dto.getToAddress().getHouse())
                    .toFlat(dto.getToAddress().getFlat());
        }

        return builder.build();
    }
}
