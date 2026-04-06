package org.example._9javaspringjsvue.service;

import org.example._9javaspringjsvue.dto.OrderDTO;
import org.example._9javaspringjsvue.entity.*;
import org.example._9javaspringjsvue.repository.CartItemRepository;
import org.example._9javaspringjsvue.repository.CartRepository;
import org.example._9javaspringjsvue.repository.OrderRepository;
import org.example._9javaspringjsvue.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartService cartService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductService productService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Cart testCart;
    private Product testProduct;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@example.com");
        testUser.setFirstName("Ivan");
        testUser.setLastName("Ivanov");

        testProduct = new Product();
        testProduct.setId(100L);
        testProduct.setTitle("IPhone 17");
        testProduct.setBasePrice(BigDecimal.valueOf(100000));
        testProduct.setDiscountPrice(BigDecimal.valueOf(90000));
        testProduct.setStockQuantity(10);

        testCartItem = new CartItem();
        testCartItem.setId(50L);
        testCartItem.setProduct(testProduct);
        testCartItem.setQuantity(2);

        testCart = new Cart();
        testCart.setId(10L);
        testCart.setUser(testUser);
        List<CartItem> items = new ArrayList<>();
        items.add(testCartItem);
        testCart.setItems(items);
    }

    @Test
    void createOrder_ShouldSuccess_WhenStockIsSufficient() {
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        Order savedOrder = new Order();
        savedOrder.setId(999L);
        savedOrder.setUser(testUser);
        savedOrder.setStatus(OrderStatus.valueOf("NEW"));
        savedOrder.setCreatedAt(ZonedDateTime.now());
        savedOrder.setTotalAmount(BigDecimal.valueOf(180000));

        List<OrderItem> orderItems = new ArrayList<>();
        OrderItem item = new OrderItem();
        item.setProduct(testProduct);
        item.setQuantity(2);
        item.setPriceAtPurchase(BigDecimal.valueOf(90000));
        orderItems.add(item);
        savedOrder.setItems(orderItems);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderDTO result = orderService.createOrder(testUser.getId());

        assertNotNull(result, "Результат не должен быть null!");
        assertEquals(999L, result.getId(), "ID заказа не совпадает");
        assertEquals(BigDecimal.valueOf(180000), result.getTotalAmount(), "Сумма заказа не совпадает");

        verify(productService, times(1)).decreaseStock(eq(100L), eq(2));
        verify(cartService, times(1)).clearCart(testUser.getId());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldThrowException_WhenStockIsInsufficient() {
        testProduct.setStockQuantity(1);

        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(testUser.getId());
        });

        assertTrue(exception.getMessage().contains("недоступен") || exception.getMessage().contains("Недостаточно"));

        verify(orderRepository, never()).save(any(Order.class));
        verify(cartService, never()).clearCart(anyLong());
    }

    @Test
    void createOrder_ShouldThrowException_WhenCartIsEmpty() {
        testCart.setItems(new ArrayList<>());

        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(testUser.getId());
        });

        assertEquals("Невозможно оформить пустую корзину", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getUserOrders_ShouldReturnListOfOrders() {
        List<Order> orders = new ArrayList<>();
        Order order = new Order();
        order.setId(1L);
        order.setUser(testUser);
        order.setTotalAmount(BigDecimal.valueOf(1000));
        orders.add(order);

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUser(testUser)).thenReturn(orders);

        List<OrderDTO> result = orderService.getUserOrders(testUser.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }
}
