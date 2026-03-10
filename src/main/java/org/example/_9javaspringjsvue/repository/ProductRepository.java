package org.example._9javaspringjsvue.repository;

import org.example._9javaspringjsvue.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Товары категории
    @Query("""
        SELECT DISTINCT p FROM Product p 
        JOIN p.categories c 
        WHERE c.id = :categoryId
    """)
    List<Product> findByCategoryId(@Param("categoryId") Long categoryId);

    // Сортировка по цене (дешевле)
    @Query("""
        SELECT DISTINCT p FROM Product p 
        JOIN p.categories c 
        WHERE c.id = :categoryId 
        ORDER BY p.basePrice ASC
    """)
    List<Product> findByCategoryIdOrderByPriceAsc(@Param("categoryId") Long categoryId);

    // Сортировка по цене (дороже)
    @Query("""
        SELECT DISTINCT p FROM Product p 
        JOIN p.categories c 
        WHERE c.id = :categoryId 
        ORDER BY p.basePrice DESC
    """)
    List<Product> findByCategoryIdOrderByPriceDesc(@Param("categoryId") Long categoryId);

    // Фильтр по оценке (через подзапрос)
    @Query("""
        SELECT p FROM Product p 
        WHERE p.id IN (
            SELECT r.product.id FROM Review r 
            GROUP BY r.product.id 
            HAVING AVG(r.rating) >= :minRating
        )
    """)
    List<Product> findByMinRating(@Param("minRating") Double minRating);

    // Наличие товара
    List<Product> findByStockQuantityGreaterThan(int quantity);

    // Поиск по названию
    List<Product> findByTitleContainingIgnoreCase(String title);

    // Карточка товара с категориями и атрибутами
    @Query("""
        SELECT DISTINCT p FROM Product p 
        LEFT JOIN FETCH p.categories 
        LEFT JOIN FETCH p.attributes 
        WHERE p.id = :productId
    """)
    Product findByIdWithCategoriesAndAttributes(@Param("productId") Long productId);
}