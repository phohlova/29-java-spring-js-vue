package org.example._9javaspringjsvue.service;

import org.example._9javaspringjsvue.dto.CartDTO;
import org.example._9javaspringjsvue.dto.CartItemDTO;
import org.example._9javaspringjsvue.dto.CartItemRequest;
import org.example._9javaspringjsvue.dto.CartSummaryDTO;
import org.example._9javaspringjsvue.entity.Cart;
import org.example._9javaspringjsvue.entity.CartItem;
import org.example._9javaspringjsvue.entity.Product;
import org.example._9javaspringjsvue.entity.User;
import org.example._9javaspringjsvue.repository.CartRepository;
import org.example._9javaspringjsvue.repository.CartItemRepository;
import org.example._9javaspringjsvue.repository.ProductRepository;
import org.example._9javaspringjsvue.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository,
                       ProductService productService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.productService = productService;
    }

    /**
     * ТЗ: "Корзина отображается всегда"
     * Получение корзины текущего пользователя
     */
    @Transactional(readOnly = true)
    public CartDTO getCartByUserId(Long userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);

        if (cartOpt.isEmpty()) {
            // Если корзины нет — создаём новую
            Cart newCart = createCartForUser(userId);
            return mapToDTO(newCart);
        }

        return mapToDTO(cartOpt.get());
    }

    /**
     * ТЗ: "в скобках указывается количество добавленных в неё товаров"
     * Получение счётчика товаров в корзине (для иконки)
     */
    @Transactional(readOnly = true)
    public Integer getCartItemCount(Long userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);

        if (cartOpt.isEmpty()) {
            return 0;
        }

        return cartItemRepository.countByCartId(cartOpt.get().getId());
    }

    /**
     * ТЗ: "Кнопка «В корзину»... товар добавляется в корзину"
     * Добавление товара в корзину
     */
    @Transactional
    public CartDTO addItemToCart(Long userId, CartItemRequest request) {
        // 1. Проверка наличия товара (ТЗ п.3.6.f)
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        if (!productService.canAddToCart(product.getId(), request.getQuantity())) {
            throw new RuntimeException("Товара нет в наличии");
        }

        // 2. Найти или создать корзину
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createCartForUser(userId));

        // 3. Найти товар в корзине
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductId(
                cart.getId(),
                product.getId()
        );

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.getQuantity();

            if (newQuantity > product.getStockQuantity()) {
                throw new RuntimeException("Недостаточно товара на складе");
            }

            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(request.getQuantity());
            cartItemRepository.save(newItem);
        }

        // 4. Возвращаем обновлённую корзину
        return mapToDTO(cartRepository.findByUserId(userId).orElseThrow());
    }

    /**
     * ТЗ: "изменить количество заказываемых товаров путём нажатия на кнопки «+» / «-»"
     * Обновление количества товара в корзине
     */
    @Transactional
    public CartDTO updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Корзина не найдена"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Товар в корзине не найден"));

        // ТЗ: "При нажатии «-» в случае, если был добавлен 1 товар, товар удаляется из корзины"
        if (quantity <= 0) {
            cartItemRepository.delete(cartItem);
        } else {
            // Проверка наличия на складе
            if (quantity > cartItem.getProduct().getStockQuantity()) {
                throw new RuntimeException("Недостаточно товара на складе");
            }

            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
        }

        return mapToDTO(cartRepository.findByUserId(userId).orElseThrow());
    }

    /**
     * ТЗ: "Товар также можно удалить из корзины, нажав на кнопку «Удалить»"
     * Удаление товара из корзины
     */
    @Transactional
    public CartDTO removeItem(Long userId, Long cartItemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Корзина не найдена"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Товар в корзине не найден"));

        cartItemRepository.delete(cartItem);
        cartItemRepository.flush();

        Cart updatedCart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Корзина не найдена"));

        return mapToDTO(cartRepository.findByUserId(userId).orElseThrow());
    }

    /**
     * ТЗ: "сводная информация по заказу – количество товаров и общая сумма"
     * Получение сводной информации о корзине
     */
    @Transactional(readOnly = true)
    public CartSummaryDTO getCartSummary(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Корзина не найдена"));

        int totalItems = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            totalItems += item.getQuantity();

            BigDecimal price = item.getProduct().getDiscountPrice() != null
                    ? item.getProduct().getDiscountPrice()
                    : item.getProduct().getBasePrice();

            totalAmount = totalAmount.add(
                    price.multiply(BigDecimal.valueOf(item.getQuantity()))
            );
        }

        return new CartSummaryDTO(totalItems, totalAmount);
    }

    /**
     * Очистка корзины (после оформления заказа)
     */
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Корзина не найдена"));

        cartItemRepository.deleteByCartId(cart.getId());
    }

    /**
     * Создание новой корзины для пользователя
     */
    private Cart createCartForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    /**
     * Маппинг Entity → DTO
     */
    private CartDTO mapToDTO(Cart cart) {
        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUser().getId());

        List<CartItemDTO> itemsDTO = cart.getItems().stream()
                .map(this::mapItemToDTO)
                .collect(Collectors.toList());
        dto.setItems(itemsDTO);

        // Подсчёт итогов
        int totalItems = itemsDTO.stream()
                .mapToInt(CartItemDTO::getQuantity)
                .sum();
        dto.setTotalItems(totalItems);

        BigDecimal totalAmount = itemsDTO.stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalAmount(totalAmount);

        return dto;
    }

    /**
     * Маппинг CartItem → CartItemDTO
     */
    private CartItemDTO mapItemToDTO(CartItem cartItem) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(cartItem.getId());
        dto.setProductId(cartItem.getProduct().getId());
        dto.setProductTitle(cartItem.getProduct().getTitle());
        dto.setProductImageUrl(cartItem.getProduct().getImageUrl());
        dto.setQuantity(cartItem.getQuantity());

        Product product = cartItem.getProduct();
        BigDecimal price = product.getDiscountPrice() != null
                ? product.getDiscountPrice()
                : product.getBasePrice();
        dto.setPrice(price);

        dto.setSubtotal(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())));

        dto.setAvailabilityStatus(productService.getAvailabilityStatus(product.getStockQuantity()));

        return dto;
    }
}