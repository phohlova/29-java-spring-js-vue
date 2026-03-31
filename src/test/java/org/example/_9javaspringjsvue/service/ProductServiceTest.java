package org.example._9javaspringjsvue.service;

import org.example._9javaspringjsvue.dto.ProductDTO;
import org.example._9javaspringjsvue.entity.Product;
import org.example._9javaspringjsvue.repository.ProductRepository;
import org.example._9javaspringjsvue.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ProductService productService;

    private Product productCheap;
    private Product productExpensive;
    private Product productOutOfStock;
    private Product productLowStock;
    private Product productInStock;

    @BeforeEach
    void setUp() {
        // --- 1. Создаем тестовые данные (ТОЛЬКО СОЗДАНИЕ, БЕЗ when()) ---

        productCheap = new Product();
        productCheap.setId(1L);
        productCheap.setTitle("Чехол");
        productCheap.setBasePrice(BigDecimal.valueOf(1000));
        productCheap.setDiscountPrice(BigDecimal.valueOf(800));
        productCheap.setStockQuantity(100);
        productCheap.setCreatedAt(LocalDateTime.now());

        productExpensive = new Product();
        productExpensive.setId(2L);
        productExpensive.setTitle("iPhone 17");
        productExpensive.setBasePrice(BigDecimal.valueOf(100000));
        productExpensive.setDiscountPrice(null);
        productExpensive.setStockQuantity(3);
        productExpensive.setCreatedAt(LocalDateTime.now());

        // Товар: Много (10 шт) -> ID 99
        productInStock = new Product();
        productInStock.setId(99L);
        productInStock.setTitle("iPhone 17 (Много)");
        productInStock.setBasePrice(BigDecimal.valueOf(100000));
        productInStock.setStockQuantity(10);

        // Товар: Мало (3 шт) -> ID 98
        productLowStock = new Product();
        productLowStock.setId(98L);
        productLowStock.setTitle("Samsung Flip (Мало)");
        productLowStock.setBasePrice(BigDecimal.valueOf(120000));
        productLowStock.setStockQuantity(3);

        // Товар: Нет (0 шт) -> ID 97
        productOutOfStock = new Product();
        productOutOfStock.setId(97L);
        productOutOfStock.setTitle("Чехол (Нет)");
        productOutOfStock.setBasePrice(BigDecimal.valueOf(1000));
        productOutOfStock.setStockQuantity(0);
    }

    @Test
    void getProductsByCategory_ShouldSortByPriceAsc_WhenAuthorized() {
        List<Product> mockProductList = Arrays.asList(productCheap, productExpensive);
        Long categoryId = 1L;

        when(productRepository.findByCategoriesIdOrderByBasePriceAsc(categoryId))
                .thenReturn(mockProductList);

        when(reviewRepository.getAverageRating(productCheap.getId())).thenReturn(4.5);
        when(reviewRepository.getAverageRating(productExpensive.getId())).thenReturn(4.0);

        List<ProductDTO> result = productService.getProductsByCategory(categoryId, "price_asc", true);

        assertEquals(2, result.size());
        assertEquals(productCheap.getId(), result.get(0).getId());
        assertEquals(productExpensive.getId(), result.get(1).getId());
    }

    @Test
    void getProductsByCategory_ShouldShowBasePrice_WhenNotAuthorized() {
        List<Product> products = Collections.singletonList(productCheap);

        when(productRepository.findByCategoryId(1L)).thenReturn(products);
        when(reviewRepository.getAverageRating(anyLong())).thenReturn(5.0);

        List<ProductDTO> result = productService.getProductsByCategory(1L, null, false);

        assertNotNull(result);
        assertFalse(result.isEmpty(), "Список товаров не должен быть пустым");

        assertEquals(BigDecimal.valueOf(1000), result.get(0).getPrice());
        assertEquals(BigDecimal.valueOf(800), result.get(0).getDiscountPrice());
    }

    @Test
    void mapToDTO_ShouldSetCorrectAvailabilityStatus() {
        when(productRepository.getProductWithDetails(99L)).thenReturn(productInStock);
        when(productRepository.getProductWithDetails(98L)).thenReturn(productLowStock);
        when(productRepository.getProductWithDetails(97L)).thenReturn(productOutOfStock);

        when(reviewRepository.getAverageRating(99L)).thenReturn(4.5);
        when(reviewRepository.getAverageRating(98L)).thenReturn(4.0);
        when(reviewRepository.getAverageRating(97L)).thenReturn(0.0);

        // Тест 1: Проверка статуса "В наличии" (ID 99)
        ProductDTO dtoHigh = productService.getProductById(99L, false);
        assertNotNull(dtoHigh);
        assertEquals("В наличии", dtoHigh.getAvailabilityStatus());
        assertEquals(99L, dtoHigh.getId());

        // Тест 2: Проверка статуса "Мало" (ID 98)
        ProductDTO dtoLow = productService.getProductById(98L, false);
        assertNotNull(dtoLow);
        assertEquals("Мало", dtoLow.getAvailabilityStatus());
        assertEquals(98L, dtoLow.getId());

        // Тест 3: Проверка статуса "Нет в наличии" (ID 97)
        ProductDTO dtoEmpty = productService.getProductById(97L, false);
        assertNotNull(dtoEmpty);
        assertEquals("Нет в наличии", dtoEmpty.getAvailabilityStatus());
        assertEquals(97L, dtoEmpty.getId());

        verify(productRepository, times(1)).getProductWithDetails(99L);
        verify(productRepository, times(1)).getProductWithDetails(98L);
        verify(productRepository, times(1)).getProductWithDetails(97L);
    }

    @Test
    void findByMinRating_ShouldFilterProducts() {
        List<Product> filteredProductsFromDb = Collections.singletonList(productCheap);

        when(productRepository.getProductsByMinRating(3.5)).thenReturn(filteredProductsFromDb);
        when(reviewRepository.getAverageRating(1L)).thenReturn(4.0);

        List<ProductDTO> result = productService.findByMinRating(3.5, false);

        assertNotNull(result);
        assertEquals(1, result.size(), "Должен вернуться 1 товар с рейтингом >= 3.5");
        assertEquals(1L, result.get(0).getId(), "Должен вернуться товар Чехол (ID 1)");
    }

    @Test
    void decreaseStock_ShouldReduceQuantity() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(productCheap));

        productService.decreaseStock(1L, 5);

        assertEquals(95, productCheap.getStockQuantity());
        verify(productRepository, times(1)).save(productCheap);
    }

    @Test
    void decreaseStock_ShouldThrowException_IfNotEnoughStock() {
        productCheap.setStockQuantity(2); // Всего 2 штуки
        when(productRepository.findById(1L)).thenReturn(Optional.of(productCheap));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            productService.decreaseStock(1L, 5); // Пытаемся списать 5
        });

        assertTrue(exception.getMessage().contains("Недостаточно"),
                "Сообщение должно содержать 'Недостаточно'. Получено: " + exception.getMessage());

        verify(productRepository, never()).save(any(Product.class));
    }
}