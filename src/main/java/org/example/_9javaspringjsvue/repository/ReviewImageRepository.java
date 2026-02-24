package org.example._9javaspringjsvue.repository;

import jakarta.transaction.Transactional;
import org.example._9javaspringjsvue.entity.Review;
import org.example._9javaspringjsvue.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {
    // Найти все фотографии отзыва
    List<ReviewImage> findByReview(Review review);

    // Удалить все фотографии отзыва
    @Modifying
    @Transactional
    void deleteByReview(Review review);

    // Удалить конкретную фотографию
    @Modifying
    @Transactional
    @Query("DELETE FROM ReviewImage ri WHERE ri.id = :id")
    int deleteImage(@Param("id") Long id);
}
