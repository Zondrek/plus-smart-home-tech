package ru.yandex.practicum.commerce.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import ru.yandex.practicum.commerce.feign.WarehouseClient;

@SpringBootApplication
@EnableFeignClients(clients = WarehouseClient.class)
public class ShoppingCartApp {

    public static void main(String[] args) {
        SpringApplication.run(ShoppingCartApp.class, args);
    }
}
