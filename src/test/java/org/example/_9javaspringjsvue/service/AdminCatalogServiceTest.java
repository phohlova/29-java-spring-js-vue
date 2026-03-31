package org.example._9javaspringjsvue.service;

import org.example._9javaspringjsvue.entity.*;
import org.example._9javaspringjsvue.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCatalogServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AttributeRepository attributeRepository;

    @Mock
    private ProductAttributeRepository productAttributeRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewImageRepository reviewImageRepository;

    @InjectMocks
    private AdminCatalogService adminCatalogService;

    private Category rootCategory;
    private Category childCategory;
    private Product testProduct;
    private Attribute testAttribute;
    private Review testReview;

    @BeforeEach
    void setUp() {

        rootCategory = new Category();
        rootCategory.setId(1L);
        rootCategory.setName("Смартфоны");
        rootCategory.setSlug("smartphones");
        rootCategory.setSortOrder(1);
        rootCategory.setParent(null);
        rootCategory.setProducts(new ArrayList<>());

        childCategory = new Category();
        childCategory.setId(2L);
        childCategory.setName("Apple");
        childCategory.setSlug("apple");
        childCategory.setParent(rootCategory);
        childCategory.setProducts(new ArrayList<>());

        testProduct = new Product();
        testProduct.setId(100L);
        testProduct.setTitle("iPhone 17");
        testProduct.setBasePrice(BigDecimal.valueOf(100000));
        testProduct.setStockQuantity(10);
        testProduct.setCategories(new ArrayList<>(Collections.singletonList(rootCategory)));

        testAttribute = new Attribute();
        testAttribute.setId(1L);
        testAttribute.setName("Цвет");
        testAttribute.setValueType("STRING");

        testReview = new Review();
        testReview.setId(50L);
        testReview.setCommentText("Хороший товар");
        testReview.setIsDeleted(false);
    }

    @Test
    void createCategory_ShouldSuccess_WithValidData() {
        when(categoryRepository.findBySlug("new-slug")).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArguments()[0]);

        Category result = adminCatalogService.createCategory("Новая категория", "new-slug", 5, 1L);

        assertNotNull(result);
        assertEquals("Новая категория", result.getName());
        assertEquals("new-slug", result.getSlug());
        assertEquals(5, result.getSortOrder());
        assertEquals(rootCategory, result.getParent());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void createCategory_ShouldThrowException_IfSlugExists() {
        when(categoryRepository.findBySlug("existing-slug")).thenReturn(Optional.of(new Category()));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            adminCatalogService.createCategory("Дубль", "existing-slug", 1, null);
        });
        assertEquals("Категория с таким slug уже существует", exception.getMessage());
    }

    @Test
    void updateCategory_ShouldMoveToNewParent() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(childCategory));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArguments()[0]);

        Category result = adminCatalogService.updateCategory(2L, "Обновленное Apple", null, null, 1L);

        assertEquals("Обновленное Apple", result.getName());
        assertEquals(rootCategory, result.getParent());
        verify(categoryRepository, times(1)).save(childCategory);
    }

    @Test
    void updateCategory_ShouldThrowException_IfSelfParent() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            adminCatalogService.updateCategory(1L, "Name", "slug", 1, 1L);
        });

        assertTrue(exception.getMessage().contains("Категория не может быть родителем самой себя"),
                "Сообщение: " + exception.getMessage());
    }

    @Test
    void deleteCategory_ShouldSuccess_IfEmpty() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));

        adminCatalogService.deleteCategory(1L);

        verify(categoryRepository, times(1)).delete(rootCategory);
    }

    @Test
    void deleteCategory_ShouldThrowException_IfNotEmpty() {
        rootCategory.getProducts().add(testProduct);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(rootCategory));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            adminCatalogService.deleteCategory(1L);
        });
        assertTrue(exception.getMessage().contains("Нельзя удалить категорию, в которой есть товары"));
        verify(categoryRepository, never()).delete(any());
    }


    @Test
    void createProduct_ShouldSuccess_WithImageAndCategories() throws IOException {
        MultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "data".getBytes());
        List<Long> catIds = Arrays.asList(1L, 2L);
        List<Category> cats = Arrays.asList(rootCategory, childCategory);

        when(categoryRepository.findAllById(catIds)).thenReturn(cats);
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArguments()[0]);

        Product result = adminCatalogService.createProduct(
                "New Phone", "Desc", null, 50000.0, 45000.0, 20, catIds, mockFile
        );

        assertNotNull(result);
        assertEquals("New Phone", result.getTitle());
        assertEquals(BigDecimal.valueOf(50000.0), result.getBasePrice());
        assertEquals(BigDecimal.valueOf(45000.0), result.getDiscountPrice());
        assertTrue(result.getImageUrl().contains("/uploads/products/")); // Проверка пути
        assertEquals(2, result.getCategories().size());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void createProduct_ShouldThrowException_IfCategoriesNotFound() {
        List<Long> catIds = Arrays.asList(999L);
        when(categoryRepository.findAllById(catIds)).thenReturn(Collections.emptyList());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            adminCatalogService.createProduct("Phone", "Desc", null, 100.0, null, 10, catIds, null);
        });
        assertTrue(exception.getMessage().contains("Не все указанные категории найдены"));
    }

    @Test
    void deleteProduct_ShouldRemoveAttributesAndProduct() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        adminCatalogService.deleteProduct(100L);

        verify(productAttributeRepository, times(1)).deleteByProduct(testProduct);
        verify(productRepository, times(1)).delete(testProduct);
    }


    @Test
    void addAttributeToProduct_ShouldCreateNewAttributeIfNotExists() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(attributeRepository.findByName("Вес")).thenReturn(Optional.empty());
        when(attributeRepository.save(any(Attribute.class))).thenAnswer(i -> {
            Attribute a = (Attribute) i.getArguments()[0];
            a.setId(99L);
            return a;
        });
        when(productAttributeRepository.existsById(any())).thenReturn(false);
        when(productAttributeRepository.save(any(ProductAttribute.class))).thenAnswer(i -> i.getArguments()[0]);

        adminCatalogService.addAttributeToProduct(100L, "Вес", "200г");

        verify(attributeRepository, times(1)).save(any(Attribute.class)); // Новый атрибут создан
        verify(productAttributeRepository, times(1)).save(any(ProductAttribute.class));
    }

    @Test
    void addAttributeToProduct_ShouldUpdateExistingAttribute() {
        ProductAttribute existingPa = new ProductAttribute();
        existingPa.setValue("Старое значение");

        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(attributeRepository.findByName("Цвет")).thenReturn(Optional.of(testAttribute));

        ProductAttributeId pk = new ProductAttributeId(100L, 1L);
        when(productAttributeRepository.existsById(pk)).thenReturn(true);
        when(productAttributeRepository.findById(pk)).thenReturn(Optional.of(existingPa));
        when(productAttributeRepository.save(any(ProductAttribute.class))).thenAnswer(i -> i.getArguments()[0]);

        adminCatalogService.addAttributeToProduct(100L, "Цвет", "Новый черный");

        assertEquals("Новый черный", existingPa.getValue());
        verify(attributeRepository, never()).save(any(Attribute.class)); // Не создавали новый
        verify(productAttributeRepository, times(1)).save(existingPa);
    }


    @Test
    void deleteReviewAsAdmin_ShouldSetDeletedFlagTrue() {
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArguments()[0]);

        adminCatalogService.deleteReviewAsAdmin(50L);

        assertTrue(testReview.getIsDeleted());
        verify(reviewRepository, times(1)).save(testReview);
    }

    @Test
    void hardDeleteReview_ShouldRemoveImagesAndReview() {
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(testReview));

        adminCatalogService.hardDeleteReview(50L);

        verify(reviewImageRepository, times(1)).deleteByReview(testReview);
        verify(reviewRepository, times(1)).delete(testReview);
    }
}