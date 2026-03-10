package org.example._9javaspringjsvue.service;

import org.example._9javaspringjsvue.dto.ProductDTO;
import org.example._9javaspringjsvue.dto.ProductAttributeDTO;
import org.example._9javaspringjsvue.entity.Product;
import org.example._9javaspringjsvue.entity.ProductAttribute;
import org.example._9javaspringjsvue.repository.ProductRepository;
import org.example._9javaspringjsvue.repository.ProductAttributeRepository;
import org.example._9javaspringjsvue.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductAttributeRepository attributeRepository;
    private final ReviewRepository reviewRepository;

    public ProductService(ProductRepository productRepository,
                          ProductAttributeRepository attributeRepository,
                          ReviewRepository reviewRepository) {
        this.productRepository = productRepository;
        this.attributeRepository = attributeRepository;
        this.reviewRepository = reviewRepository;
    }

    /**
     * ТЗ: "Листинг товаров (главная страница)"
     * Получение товаров категории с сортировкой и проверкой авторизации
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(Long categoryId, String sort, Boolean isAuthorized) {
        List<Product> products;

        // ТЗ: "сортировка - сначала дешевле / сначала дороже"
        if ("price_asc".equals(sort)) {
            products = productRepository.findByCategoryIdOrderByPriceAsc(categoryId);
        } else if ("price_desc".equals(sort)) {
            products = productRepository.findByCategoryIdOrderByPriceDesc(categoryId);
        } else {
            products = (List<Product>) productRepository.findByCategoryId(categoryId);
        }

        // Преобразуем в DTO с расчётом цен
        return products.stream()
                .map(product -> mapToDTO(product, isAuthorized))
                .collect(Collectors.toList());
    }

    /**
     * Получение всех товаров (для админки)
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts(Boolean isAuthorized) {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(product -> mapToDTO(product, isAuthorized))
                .collect(Collectors.toList());
    }

    /**
     * ТЗ: "Карточка товара"
     * Получение детальной информации о товаре
     */
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long productId, Boolean isAuthorized) {
        Product product = productRepository.findByIdWithCategoriesAndAttributes(productId);

        return mapToDTO(product, isAuthorized);
    }

    /**
     * ТЗ: "Фильтрация по оценке"
     * Поиск товаров с минимальной оценкой
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> findByMinRating(Double minRating, Boolean isAuthorized) {
        List<Product> products = productRepository.findByMinRating(minRating);
        return products.stream()
                .map(product -> mapToDTO(product, isAuthorized))
                .collect(Collectors.toList());
    }

    /**
     * ТЗ: "Поиск по характеристикам"
     * Фильтрация товаров по атрибутам
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> findByAttributes(String attrName, String attrValue, Boolean isAuthorized) {
        List<ProductAttribute> productAttributes =
                attributeRepository.findByAttributeNameAndValue(attrName, attrValue);

        List<Long> productIds = productAttributes.stream()
                .map(pa -> pa.getProduct().getId())
                .distinct()
                .collect(Collectors.toList());

        List<Product> products = productRepository.findAllById(productIds);
        return products.stream()
                .map(product -> mapToDTO(product, isAuthorized))
                .collect(Collectors.toList());
    }

    /**
     * ТЗ: "Информация о наличии товара"
     * Возвращает статус наличия
     */
    public String getAvailabilityStatus(Integer stockQuantity) {
        if (stockQuantity == null || stockQuantity == 0) {
            return "Нет в наличии";
        } else if (stockQuantity >= 1 && stockQuantity <= 5) {
            return "Мало";
        } else {
            return "В наличии";
        }
    }

    /**
     * ТЗ: "Кнопка В корзину (проверка наличия)"
     * Проверка, можно ли добавить товар в корзину
     */
    @Transactional(readOnly = true)
    public boolean canAddToCart(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        // Проверяем, что товара достаточно на складе
        return product.getStockQuantity() >= quantity;
    }

    /**
     * ТЗ: "Уменьшение остатков при оформлении заказа"
     * Уменьшение количества товара на складе
     */
    @Transactional
    public void decreaseStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException("Недостаточно товара на складе");
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
    }

    /**
     * Получение средней оценки товара
     */
    @Transactional(readOnly = true)
    public Double getAverageRating(Long productId) {
        return reviewRepository.getAverageRating(productId);
    }


    /**
     * Маппинг Entity → DTO с расчётом цены для пользователя
     */
    private ProductDTO mapToDTO(Product product, Boolean isAuthorized) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setTitle(product.getTitle());
        dto.setDescription(product.getDescription());
        dto.setImageUrl(product.getImageUrl());
        dto.setBasePrice(product.getBasePrice());
        dto.setDiscountPrice(product.getDiscountPrice());
        dto.setStockQuantity(product.getStockQuantity());

        // ТЗ: "Цена для авторизованных/неавторизованных"
        if (isAuthorized && product.getDiscountPrice() != null) {
            dto.setPrice(product.getDiscountPrice());
        } else {
            dto.setPrice(product.getBasePrice());
        }

        // ТЗ: "Информация о наличии"
        dto.setAvailabilityStatus(getAvailabilityStatus(product.getStockQuantity()));

        // Средняя оценка
        Double avgRating = reviewRepository.getAverageRating(product.getId());
        dto.setAverageRating(avgRating != null ? avgRating : 0.0);

        return dto;
    }
}
