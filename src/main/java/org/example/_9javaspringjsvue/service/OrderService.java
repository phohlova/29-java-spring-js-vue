package org.example._9javaspringjsvue.service;

import org.example._9javaspringjsvue.dto.OrderDTO;
import org.example._9javaspringjsvue.dto.OrderItemDTO;
import org.example._9javaspringjsvue.entity.*;
import org.example._9javaspringjsvue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        CartService cartService,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        ProductService productService,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productService = productService;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrderDTO createOrder(Long userId) {
        // 1. Получаем корзину
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Корзина пуста или не найдена"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Невозможно оформить пустую корзину");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 2. Создаем заказ
        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.valueOf("NEW")); // Используем строку, так как в Entity у вас String
        order.setCreatedAt(ZonedDateTime.now());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 3. Обработка позиций
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();

            // Проверка наличия
            if (product.getStockQuantity() < quantity) {
                throw new RuntimeException(
                        "Товар \"" + product.getTitle() + "\" недоступен в количестве " + quantity +
                                ". Доступно: " + product.getStockQuantity()
                );
            }

            // Фиксация цены
            BigDecimal price = (product.getDiscountPrice() != null && product.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0)
                    ? product.getDiscountPrice()
                    : product.getBasePrice();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setPriceAtPurchase(price);

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(quantity)));

            // Уменьшение остатков
            productService.decreaseStock(product.getId(), quantity);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);

        // Сохранение заказа
        Order savedOrder = orderRepository.save(order);

        // Очистка корзины
        cartService.clearCart(userId);

        System.out.println(">>> ЗАКАЗ №" + savedOrder.getId() + " УСПЕШНО СОЗДАН ДЛЯ " + user.getEmail());
        System.out.println(">>> Письмо не отправлено, так как почтовый сервис не подключен (для избежания ошибки запуска).");

        return mapToDTO(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getUserOrders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Order> orders = orderRepository.findByUser(user);
        return orders.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private OrderDTO mapToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(String.valueOf(order.getStatus()));
        dto.setCreatedAt(order.getCreatedAt());

        List<OrderItemDTO> itemsDTO = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                OrderItemDTO itemDTO = new OrderItemDTO();
                itemDTO.setProductId(item.getProduct().getId());
                itemDTO.setProductTitle(item.getProduct().getTitle());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setPrice(item.getPriceAtPurchase());
                itemDTO.setSubtotal(item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())));
                itemsDTO.add(itemDTO);
            }
        }
        dto.setItems(itemsDTO);
        return dto;
    }
}