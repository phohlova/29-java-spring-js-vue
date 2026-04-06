package org.example._9javaspringjsvue.controller;

import org.example._9javaspringjsvue.dto.CartDTO;
import org.example._9javaspringjsvue.dto.CartItemRequest;
import org.example._9javaspringjsvue.entity.User;
import org.example._9javaspringjsvue.repository.UserRepository;
import org.example._9javaspringjsvue.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    public CartController(CartService cartService, UserRepository userRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
    }

    /**
     * ТЗ: корзина отображается всегда (для авторизованных)
     * Получение корзины текущего пользователя
     * GET /api/cart
     */
    @GetMapping
    public ResponseEntity<CartDTO> getCart(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    /**
     * ТЗ: кнопка "В корзину"
     * Добавдение товара в корзину
     * POST /api/cart/items
     */
    @PostMapping("/items")
    public ResponseEntity<CartDTO> addItem(@RequestBody CartItemRequest request, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(cartService.addItemToCart(userId, request));
    }

    /**
     * ТЗ: изменить количество... кнопки + / -
     * Обновление количества товара
     * PUT /api/cart/items/{itemId}?quantity=5
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> updateQuantity(@PathVariable Long itemId,
                                                  @RequestParam Integer quantity,
                                                  Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(cartService.updateQuantity(userId, itemId, quantity));
    }

    /**
     * ТЗ: кнопка "Удалить"
     * Удаление товара из корзины
     * DELETE /api/cart/items/{itemId}
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> removeItem(@PathVariable Long itemId,
                                              Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(cartService.removeItem(userId, itemId));
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
