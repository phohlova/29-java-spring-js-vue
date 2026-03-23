package org.example._9javaspringjsvue.controller;

import org.example._9javaspringjsvue.dto.ReviewDTO;
import org.example._9javaspringjsvue.entity.User;
import org.example._9javaspringjsvue.repository.UserRepository;
import org.example._9javaspringjsvue.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    public ReviewController(ReviewService reviewService, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    /**
     * ТЗ: для зарегистрированных пользователей - постановка оценки... комментарии с прикреплением фото
     * POST /api/reviews/products/{productId}
     */
    @PostMapping("/products/{productId}")
    public ResponseEntity<ReviewDTO> createReview(@PathVariable Long productId,
                                                  @RequestParam Integer rating,
                                                  @RequestParam(required = false) String commentText,
                                                  @RequestParam(required = false)List<MultipartFile> images,
                                                  Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(reviewService.createReview(userId, productId, rating, commentText, images));
    }

    /**
     * ТЗ: для всех пользователей - просмотр оценок и комментариев... фильтрация комментариев по оценке
     * GET /api/reviews/products/{productId}?rating=5
     */
    @GetMapping("/products/{productId}")
    public ResponseEntity<List<ReviewDTO>> getReviews(@PathVariable Long productId,
                                                      @RequestParam(required = false) Integer rating) {
        return ResponseEntity.ok(reviewService.getReviewsByProductId(productId, rating));
    }

    /**
     * ТЗ: Для админки - возможность удаления комментариев и/или фото в них
     * DELETE /api/reviews/{id}
     */
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }
        reviewService.deleteReview(id, true);
        return ResponseEntity.ok().build();
    }

    /**
     * Удаление конкретного фото из отзыва (для админа)
     * DELETE /api/reviews/images/{imageId}
     */
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId,
                                            Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }
        reviewService.deleteImage(imageId);
        return ResponseEntity.ok().build();
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Пользователь не авторизован");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + email));
        return user.getId();
    }

    private  boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
