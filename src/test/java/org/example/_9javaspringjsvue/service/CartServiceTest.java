package org.example._9javaspringjsvue.service;

import org.example._9javaspringjsvue.dto.CartDTO;
import org.example._9javaspringjsvue.dto.CartItemRequest;
import org.example._9javaspringjsvue.entity.Cart;
import org.example._9javaspringjsvue.entity.CartItem;
import org.example._9javaspringjsvue.entity.Product;
import org.example._9javaspringjsvue.entity.User;
import org.example._9javaspringjsvue.repository.CartItemRepository;
import org.example._9javaspringjsvue.repository.CartRepository;
import org.example._9javaspringjsvue.repository.ProductRepository;
import org.example._9javaspringjsvue.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private Cart testCart;
    private Product testProduct;
    private CartItemRequest addItemRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Ivan");
        testUser.setLastName("Ivanov");

        testCart = new Cart();
        testCart.setId(10L);
        testCart.setUser(testUser);
        testCart.setItems(new ArrayList<>());

        testProduct = new Product();
        testProduct.setId(5L);
        testProduct.setTitle("IPhone 17");
        testProduct.setBasePrice(BigDecimal.valueOf(100000));
        testProduct.setDiscountPrice(BigDecimal.valueOf(95000));
        testProduct.setStockQuantity(10);

        addItemRequest = new CartItemRequest();
        addItemRequest.setProductId(5L);
        addItemRequest.setQuantity(2);
    }

    @Test
    void addItemToCart_ShouldCreateNewCartAndAddItem_WhenCartDoesNotExist() {
        testCart.setItems(new ArrayList<>());

        when(cartRepository.findByUserId(testUser.getId()))
                .thenReturn(Optional.empty(), Optional.of(testCart));
        when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
        when(productService.canAddToCart(testProduct.getId(), addItemRequest.getQuantity())).thenReturn(true);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(cartItemRepository.findByCartAndProduct(any(Cart.class), any(Product.class)))
                .thenReturn(Optional.empty());

        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cartToSave = invocation.getArgument(0);
            if (cartToSave.getId() == null) {
                cartToSave.setId(testCart.getId());
            }
            return cartToSave;
        });

        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem itemToSave = invocation.getArgument(0);
            if (itemToSave.getId() == null) {
                itemToSave.setId(999L);
            }
            testCart.getItems().add(itemToSave);
            return itemToSave;
        });

        CartDTO result = cartService.addItemToCart(testUser.getId(), addItemRequest);

        assertNotNull(result, "Результат не должен быть null");
        assertEquals(1, result.getItems().size(), "В корзине должен быть 1 товар");
        assertEquals(2, result.getTotalItems(), "Общее количество товаров должно быть 2");

        BigDecimal expectedAmount = BigDecimal.valueOf(190000);
        assertEquals(expectedAmount, result.getTotalAmount(), "Сумма заказа рассчитана неверно");

        verify(cartRepository, times(1)).save(any(Cart.class));
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
        verify(cartItemRepository, times(1)).findByCartAndProduct(any(Cart.class), any(Product.class));
    }

    @Test
    void addItemToCart_ShouldIncreaseQuantity_WhenItemAlreadyExists() {
        CartItem existingItem = new CartItem();
        existingItem.setId(100L);
        existingItem.setCart(testCart);
        existingItem.setProduct(testProduct);
        existingItem.setQuantity(1);

        testCart.getItems().add(existingItem);

        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
        when(productService.canAddToCart(testProduct.getId(), addItemRequest.getQuantity())).thenReturn(true);
        when(cartItemRepository.findByCartAndProduct(testCart, testProduct)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArguments()[0]);

        CartDTO result = cartService.addItemToCart(testUser.getId(), addItemRequest);

        assertNotNull(result);
        assertEquals(3, result.getTotalItems());

        verify(cartItemRepository, times(1)).save(existingItem);

        assertEquals(3, existingItem.getQuantity());
    }

    @Test
    void addItemToCart_ShouldThrowException_WhenProductNotEnoughStock() {
        testProduct.setStockQuantity(1);

        when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
        when(productService.canAddToCart(testProduct.getId(), addItemRequest.getQuantity())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cartService.addItemToCart(testUser.getId(), addItemRequest);
        });

        assertTrue(exception.getMessage().contains("нет в наличии"));
        verify(cartRepository, never()).save(any(Cart.class));
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void updateQuantity_ShouldSuccess_WhenValidQuantity() {
        CartItem item = new CartItem();
        item.setId(100L);
        item.setCart(testCart);
        item.setProduct(testProduct);
        item.setQuantity(2);
        testCart.getItems().add(item);

        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(100L)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArguments()[0]);

        CartDTO result = cartService.updateQuantity(testUser.getId(), 100L, 5);

        assertNotNull(result);
        assertEquals(5, result.getTotalItems());
        verify(cartItemRepository, times(1)).save(item);
    }

    @Test
    void removeItem_ShouldSuccess_AndReturnEmptyCart() {
        CartItem item = new CartItem();
        item.setId(100L);
        item.setCart(testCart);
        item.setProduct(testProduct);

        testCart.setItems(new ArrayList<>());
        testCart.getItems().add(item);

        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

        when(cartItemRepository.findById(100L)).thenReturn(Optional.of(item));

        doAnswer(invocation -> {
            CartItem deletedItem = invocation.getArgument(0);
            testCart.getItems().remove(deletedItem);
            return null;
        }).when(cartItemRepository).delete(item);

        CartDTO result = cartService.removeItem(testUser.getId(), 100L);

        assertNotNull(result);

        assertEquals(0, result.getItems().size(), "Список товаров должен быть пуст");
        assertEquals(0, result.getTotalItems(), "Общее количество должно быть 0");
        assertEquals(BigDecimal.ZERO, result.getTotalAmount(), "Сумма должна быть 0");

        verify(cartItemRepository, times(1)).delete(item);
    }
}
