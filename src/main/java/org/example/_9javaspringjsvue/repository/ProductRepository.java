package org.example._9javaspringjsvue.repository;

import org.example._9javaspringjsvue.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // Найти товары по категории (с учётом подкатегорий)
    @Query("SELECT DISTINCT p FROM Product p " +
            "JOIN p.categories c " +
            "WHERE c.id = :categoryId OR c.parent.id = :categoryId")
    Page<Product> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    // Сортировка по цене - сначала дешевле
    Page<Product> findAllByOrderByBasePriceAsc(Pageable pageable);

    // Сортировка по цене - сначала дороже
    Page<Product> findAllByOrderByBasePriceDesc(Pageable pageable);

    // Поиск по названию товара
    Page<Product> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    //  Проверить наличие товара
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.stockQuantity > 0")
    Optional<Product> findAvailableById(@Param("id") Long id);

    // Найти все товары в наличии
    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0")
    Page<Product> findAvailableProducts(Pageable pageable);

    //  Проверить доступность для добавления в корзину
    @Query("SELECT CASE WHEN p.stockQuantity > 0 THEN true ELSE false END FROM Product p WHERE p.id = :id")
    boolean isAvailable(@Param("id") Long id);

    // Уменьшить остаток товара при оформлении заказа
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity " +
            "WHERE p.id = :productId AND p.stockQuantity >= :quantity")
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    // Проверить достаточность остатков
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Product p " +
            "WHERE p.id = :productId AND p.stockQuantity >= :quantity")
    boolean hasSufficientStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    // Найти товары по характеристике
    @Query("SELECT p FROM Product p " +
            "JOIN p.attributes pa " +
            "JOIN pa.attribute a " +
            "WHERE a.name = :attributeName AND pa.value = :value")
    Page<Product> findByAttribute(@Param("attributeName") String attributeName,
                                  @Param("value") String value,
                                  Pageable pageable);

    // Найти товары по минимальному рейтингу
    @Query("SELECT p FROM Product p " +
            "WHERE EXISTS (SELECT 1 FROM Review r " +
            "WHERE r.product = p AND r.isDeleted = false " +
            "GROUP BY r.product HAVING AVG(r.rating) >= :minRating)")
    Page<Product> findByMinRating(@Param("minRating") Double minRating, Pageable pageable);

    // Найти товары без категории
    @Query("SELECT p FROM Product p WHERE SIZE(p.categories) = 0")
    List<Product> findWithoutCategories();

    // Найти товары с нулевым остатком
    @Query("SELECT p FROM Product p WHERE p.stockQuantity = 0")
    List<Product> findOutOfStock();

    // Найти товары со скидкой
    @Query("SELECT p FROM Product p WHERE p.discountPrice IS NOT NULL")
    List<Product> findWithDiscount();

    // Количество товаров в категории (с подкатегориями)
    @Query("SELECT COUNT(DISTINCT p) FROM Product p " +
            "JOIN p.categories c " +
            "WHERE c.id = :categoryId OR c.parent.id = :categoryId")
    Long countByCategoryId(@Param("categoryId") Long categoryId);

    // Средняя оценка товара
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.isDeleted = false")
    Double getAverageRating(@Param("productId") Long productId);
}
