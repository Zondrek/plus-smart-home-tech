package ru.yandex.practicum.commerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNewOrderRequest {

    @NotNull
    @Valid
    private ShoppingCartDto shoppingCart;

    @NotNull
    @Valid
    private AddressDto deliveryAddress;
}
