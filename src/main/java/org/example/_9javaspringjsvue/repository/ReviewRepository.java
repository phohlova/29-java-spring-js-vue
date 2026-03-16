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

    // Только активные отзывы
    List<Review> findByProductIdAndIsDeletedFalseOrderByCreatedAtDesc(Long productId);

    // С пагинацией (только активные)
    Page<Review> findByProductIdAndIsDeletedFalseOrderByCreatedAtDesc(Long productId, Pageable pageable);

    // Фильтр по оценке (только активные)
    List<Review> findByProductIdAndRatingAndIsDeletedFalseOrderByCreatedAtDesc(Long productId, Integer rating);

    // Средняя оценка (исключая удаленные)
    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.product.id = :productId AND r.isDeleted = false")
    Double getAverageRating(@Param("productId") Long productId);

    Long countByProductIdAndIsDeletedFalse(Long productId);

    List<Review> findByUserId(Long userId);

    // Для админки: все удаленные
    List<Review> findByIsDeletedTrue();
}