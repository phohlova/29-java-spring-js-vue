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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
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
     * Получение товаров категории с сортировкой.
     * Параметр isAuthorized нужен для расчета отображаемой цены в DTO.
     */
    public List<ProductDTO> getProductsByCategory(Long categoryId, String sort, Boolean isAuthorized) {
        List<Product> products;

        // ТЗ: "сортировка - сначала дешевле / сначала дороже"
        if ("price_asc".equals(sort)) {
            products = productRepository.getProductsByCategoryOrderByPriceAsc(categoryId);
        } else if ("price_desc".equals(sort)) {
            products = productRepository.getProductsByCategoryOrderByPriceDesc(categoryId);
        } else {
            products = productRepository.getProductsByCategory(categoryId);
        }

        return products.stream()
                .map(product -> mapToDTO(product, isAuthorized))
                .collect(Collectors.toList());
    }

    /**
     * Получение всех товаров (для админки или общего листинга без категории)
     */
    public List<ProductDTO> getAllProducts(Boolean isAuthorized) {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(product -> mapToDTO(product, isAuthorized))
                .collect(Collectors.toList());
    }

    /**
     * ТЗ: "Карточка товара"
     * Получение детальной информации о товаре с категориями и атрибутами.
     */
    public ProductDTO getProductById(Long productId, Boolean isAuthorized) {
        Product product = productRepository.getProductWithDetails(productId);

        if (product == null) {
            throw new IllegalArgumentException("Товар с ID " + productId + " не найден");
        }

        return mapToDTO(product, isAuthorized);
    }

    /**
     * ТЗ: "Фильтрация по оценке"
     * Поиск товаров с минимальной оценкой.
     */
    public List<ProductDTO> findByMinRating(Double minRating, Boolean isAuthorized) {
        if (minRating == null || minRating < 0 || minRating > 5) {
            throw new IllegalArgumentException("Рейтинг должен быть от 0 до 5");
        }
        List<Product> products = productRepository.getProductsByMinRating(minRating);
        return products.stream()
                .map(product -> mapToDTO(product, isAuthorized))
                .collect(Collectors.toList());
    }

    /**
     * ТЗ: "Поиск по характеристикам"
     * Фильтрация товаров по атрибутам (Например: Цвет=Красный).
     */
    public List<ProductDTO> findByAttributes(String attrName, String attrValue, Boolean isAuthorized) {
        if (attrName == null || attrValue == null) {
            return Collections.emptyList();
        }

        List<ProductAttribute> productAttributes =
                attributeRepository.findByAttributeNameAndValue(attrName, attrValue);

        if (productAttributes.isEmpty()) {
            return Collections.emptyList();
        }

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
     * ТЗ: "Информация о наличии товара" (п. 6.d)
     * Возвращает статус наличия: "Нет в наличии", "Мало", "В наличии".
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
     * ТЗ: "Кнопка В корзину (проверка наличия)" (п. 6.f)
     * Проверка, можно ли добавить товар в корзину в текущий момент.
     */
    public boolean canAddToCart(Long productId, Integer quantity) {
        if (quantity <= 0) {
            return false;
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        return product.getStockQuantity() >= quantity;
    }

    /**
     * ТЗ: "Уменьшение остатков при оформлении заказа"
     * Транзакционное уменьшение количества товара на складе.
     */
    @Transactional
    public void decreaseStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар не найден"));

        if (product.getStockQuantity() < quantity) {
            throw new IllegalStateException("Недостаточно товара на складе для продукта: " + product.getTitle());
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
    }

    /**
     * Получение средней оценки товара.
     */
    public Double getAverageRating(Long productId) {
        Double rating = reviewRepository.getAverageRating(productId);
        return rating != null ? rating : 0.0;
    }

    /**
     * Маппинг Entity → DTO с расчётом цены для пользователя.
     * ТЗ п. 6.e: Если авторизован и есть скидка — показываем скидочную цену.
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

        BigDecimal displayPrice;
        if (Boolean.TRUE.equals(isAuthorized) &&
                product.getDiscountPrice() != null &&
                product.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0) {
            displayPrice = product.getDiscountPrice();
        } else {
            displayPrice = product.getBasePrice();
        }
        dto.setPrice(displayPrice);

        // Статус наличия
        dto.setAvailabilityStatus(getAvailabilityStatus(product.getStockQuantity()));

        // Средняя оценка
        dto.setAverageRating(getAverageRating(product.getId()));

        return dto;
    }
}