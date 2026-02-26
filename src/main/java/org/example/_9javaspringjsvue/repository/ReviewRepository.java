package org.example._9javaspringjsvue.repository;

import jakarta.transaction.Transactional;
import org.example._9javaspringjsvue.entity.Product;
import org.example._9javaspringjsvue.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Найти все отзывы товара
    List<Review> findByProduct(Product product);

    // Найти отзывы с пагинацией
    Page<Review> findByProduct(Product product, Pageable pageable);

    // Найти не удалённые отзывы
    Page<Review> findByProductAndIsDeletedFalse(Product product, Pageable pageable);

    // Фильтрация отзывов по оценке
    Page<Review> findByProductAndRatingAndIsDeletedFalse(Product product,
                                                         Integer rating,
                                                         Pageable pageable);

    // Посчитать среднюю оценку товара
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product = :product AND r.isDeleted = false")
    Double getAverageRatingByProduct(@Param("product") Product product);

    // Посчитать количество отзывов
    long countByProductAndIsDeletedFalse(Product product);

    // Удалить отзыв
    @Modifying
    @Transactional
    @Query("UPDATE Review r SET r.isDeleted = true WHERE r.id = :id")
    int softDelete(@Param("id") Long id);
}
