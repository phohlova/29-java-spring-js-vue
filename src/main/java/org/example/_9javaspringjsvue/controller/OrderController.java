package org.example._9javaspringjsvue.controller;

import org.example._9javaspringjsvue.dto.OrderDTO;
import org.example._9javaspringjsvue.entity.User;
import org.example._9javaspringjsvue.repository.UserRepository;
import org.example._9javaspringjsvue.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    /**
     * ТЗ: оформить заказ
     * Повторная проверка наличия уменьшение остатков, очистка корзины
     * POST /api/orders
     */
    public ResponseEntity<OrderDTO> createOrder(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(orderService.createOrder(userId));
    }

    /**
     * ТЗ: история заказов
     * Получение списка заказов текущего пользователя
     * GET /api/orders
     */
    public ResponseEntity<List<OrderDTO>> getMyOrders(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Пользователь не авторизован");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + email));
        return user.getId();
    }
}
