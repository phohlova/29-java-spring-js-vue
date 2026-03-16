package org.example._9javaspringjsvue.service;

import org.example._9javaspringjsvue.entity.*;
import org.example._9javaspringjsvue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdminCatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final AttributeRepository attributeRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;

    private static final String UPLOAD_DIR = "uploads/products/";

    public AdminCatalogService(CategoryRepository categoryRepository,
                               ProductRepository productRepository,
                               AttributeRepository attributeRepository,
                               ProductAttributeRepository productAttributeRepository,
                               ReviewRepository reviewRepository,
                               ReviewImageRepository reviewImageRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.attributeRepository = attributeRepository;
        this.productAttributeRepository = productAttributeRepository;
        this.reviewRepository = reviewRepository;
        this.reviewImageRepository = reviewImageRepository;
    }

    /**
     * Создание новой категории
     */
    public Category createCategory(String name, String slug, Integer sortOrder, Long parentId) {
        if (categoryRepository.findBySlug(slug).isPresent()) {
            throw new IllegalArgumentException("Категория с таким slug уже существует");
        }

        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setSortOrder(sortOrder != null ? sortOrder : 0);

        if (parentId != null) {
            Category parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Родительская категория не найдена"));
            category.setParent(parent);
        }

        return categoryRepository.save(category);
    }

    /**
     * Обновление категории (включая перемещение в другую родительскую)
     */
    public Category updateCategory(Long id, String name, String slug, Integer sortOrder, Long parentId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

        if (name != null) category.setName(name);
        if (slug != null && !slug.equals(category.getSlug())) {
            if (categoryRepository.findBySlug(slug).isPresent()) {
                throw new IllegalArgumentException("Категория с таким slug уже существует");
            }
            category.setSlug(slug);
        }
        if (sortOrder != null) category.setSortOrder(sortOrder);

        if (parentId != null) {
            if (parentId.equals(id)) {
                throw new IllegalArgumentException("Категория не может быть родителем самой себя");
            }
            Category parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Родительская категория не найдена"));
            category.setParent(parent);
        } else if (parentId == null && category.getParent() != null) {
            // Если передан null, делаем категорию корневой
            category.setParent(null);
        }

        return categoryRepository.save(category);
    }

    /**
     * Удаление категории (вместе с продуктами? Нет, продукты нужно либо удалить, либо перенести.
     * Для простоты удаляем только если категория пуста, или удаляем каскадом, если настроено.
     * В данном примере - запрет на удаление непустой категории для безопасности).
     */
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new IllegalStateException("Нельзя удалить категорию, в которой есть товары. Переместите товары сначала.");
        }

        categoryRepository.delete(category);
    }


    /**
     * Создание товара
     */
    public Product createProduct(String title, String description, String imageUrl,
                                 Double basePrice, Double discountPrice, Integer stockQuantity,
                                 List<Long> categoryIds, MultipartFile imageFile) throws IOException {

        Product product = new Product();
        product.setTitle(title);
        product.setDescription(description);
        product.setBasePrice(java.math.BigDecimal.valueOf(basePrice));
        product.setDiscountPrice(discountPrice != null ? java.math.BigDecimal.valueOf(discountPrice) : null);
        product.setStockQuantity(stockQuantity != null ? stockQuantity : 0);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        if (imageFile != null && !imageFile.isEmpty()) {
            product.setImageUrl(saveImage(imageFile));
        } else if (imageUrl != null) {
            product.setImageUrl(imageUrl);
        }

        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(categoryIds);
            if (categories.size() != categoryIds.size()) {
                throw new IllegalArgumentException("Не все указанные категории найдены");
            }
            product.setCategories(categories);
        }

        return productRepository.save(product);
    }

    /**
     * Обновление товара
     */
    public Product updateProduct(Long id, String title, String description,
                                 Double basePrice, Double discountPrice, Integer stockQuantity,
                                 List<Long> categoryIds, MultipartFile imageFile) throws IOException {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        if (title != null) product.setTitle(title);
        if (description != null) product.setDescription(description);
        if (basePrice != null) product.setBasePrice(java.math.BigDecimal.valueOf(basePrice));
        if (discountPrice != null) product.setDiscountPrice(java.math.BigDecimal.valueOf(discountPrice));
        if (stockQuantity != null) product.setStockQuantity(stockQuantity);
        product.setUpdatedAt(LocalDateTime.now());

        if (imageFile != null && !imageFile.isEmpty()) {
            product.setImageUrl(saveImage(imageFile));
        }

        if (categoryIds != null) {
            List<Category> categories = categoryRepository.findAllById(categoryIds);
            product.setCategories(categories);
        }

        return productRepository.save(product);
    }

    /**
     * Удаление товара
     */
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        productAttributeRepository.deleteByProduct(product);

        productRepository.delete(product);
    }

    /**
     * Создание глобального атрибута (например, "Цвет", "Память")
     */
    public Attribute createAttribute(String name, String valueType) {
        if (attributeRepository.existsByName(name)) {
            throw new IllegalArgumentException("Атрибут с таким именем уже существует");
        }
        Attribute attribute = new Attribute();
        attribute.setName(name);
        attribute.setValueType(valueType != null ? valueType : "STRING");
        return attributeRepository.save(attribute);
    }

    /**
     * Добавление значения атрибута к конкретному товару
     * Пример: Товару "iPhone" добавить атрибут "Цвет" = "Черный"
     */
    public void addAttributeToProduct(Long productId, String attributeName, String value) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        Attribute attribute = attributeRepository.findByName(attributeName)
                .orElseGet(() -> {
                    Attribute newAttr = new Attribute();
                    newAttr.setName(attributeName);
                    newAttr.setValueType("STRING");
                    return attributeRepository.save(newAttr);
                });

        ProductAttributeId pk = new ProductAttributeId(productId, attribute.getId());
        if (productAttributeRepository.existsById(pk)) {
            ProductAttribute pa = productAttributeRepository.findById(pk).get();
            pa.setValue(value);
            productAttributeRepository.save(pa);
        } else {
            ProductAttribute pa = new ProductAttribute();
            pa.setId(pk);
            pa.setProduct(product);
            pa.setAttribute(attribute);
            pa.setValue(value);
            productAttributeRepository.save(pa);
        }
    }

    /**
     * Удаление атрибута у товара
     */
    public void removeAttributeFromProduct(Long productId, String attributeName) {
        Attribute attribute = attributeRepository.findByName(attributeName)
                .orElseThrow(() -> new IllegalArgumentException("Атрибут не найден"));

        ProductAttributeId pk = new ProductAttributeId(productId, attribute.getId());
        if (productAttributeRepository.existsById(pk)) {
            productAttributeRepository.deleteById(pk);
        }
    }

    /**
     * Удаление отзыва администратором (мягкое удаление)
     */
    public void deleteReviewAsAdmin(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Отзыв не найден"));

        review.setIsDeleted(true);
        reviewRepository.save(review);
    }

    /**
     * Полное удаление отзыва и его изображений
     */
    public void hardDeleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Отзыв не найден"));

        reviewImageRepository.deleteByReview(review);
        reviewRepository.delete(review);
    }

    private String saveImage(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR + fileName);

        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());

        return "/uploads/products/" + fileName;
    }
}