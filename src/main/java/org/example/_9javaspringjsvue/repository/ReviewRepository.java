package org.example._9javaspringjsvue.repository;

import org.example._9javaspringjsvue.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Отзывы товара
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    // С пагинацией
    Page<Review> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    // Фильтр по оценке
    List<Review> findByProductIdAndRatingOrderByCreatedAtDesc(Long productId, Integer rating);

    // Средняя оценка товара
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.isDeleted = false")
    Double getAverageRating(@Param("productId") Long productId);

    // Количество отзывов
    Long countByProductId(Long productId);

    // Отзывы пользователя
    List<Review> findByUserId(Long userId);

    // Удалённые отзывы (для админки)
    List<Review> findByIsDeletedTrue();

    // Активные отзывы
    List<Review> findByProductIdAndIsDeletedFalse(Long productId);
}
