package org.example._9javaspringjsvue.service;

import org.example._9javaspringjsvue.dto.ReviewDTO;
import org.example._9javaspringjsvue.dto.ReviewImageDTO;
import org.example._9javaspringjsvue.entity.Product;
import org.example._9javaspringjsvue.entity.Review;
import org.example._9javaspringjsvue.entity.ReviewImage;
import org.example._9javaspringjsvue.entity.User;
import org.example._9javaspringjsvue.repository.ProductRepository;
import org.example._9javaspringjsvue.repository.ReviewImageRepository;
import org.example._9javaspringjsvue.repository.ReviewRepository;
import org.example._9javaspringjsvue.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    private static final String UPLOAD_DIR = "uploads/reviews/";

    public ReviewService(ReviewRepository reviewRepository,
                         ReviewImageRepository imageRepository,
                         ProductRepository productRepository,
                         UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.imageRepository = imageRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    /**
     * ТЗ: "Для зарегистрированных пользователей – простановка оценки... комментарии с прикреплением фотографий"
     */
    @Transactional
    public ReviewDTO createReview(Long userId, Long productId, Integer rating, String commentText, List<MultipartFile> images) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Рейтинг должен быть от 1 до 5");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(rating);
        review.setCommentText(commentText);
        review.setIsDeleted(false);
        review.setCreatedAt(LocalDateTime.now());

        // Сохраняем отзыв, чтобы получить ID для картинок
        Review savedReview = reviewRepository.save(review);

        // Обработка изображений
        if (images != null && !images.isEmpty()) {
            List<ReviewImage> reviewImages = new ArrayList<>();
            for (MultipartFile file : images) {
                if (!file.isEmpty()) {
                    try {
                        String imageUrl = saveImage(file);
                        ReviewImage reviewImage = new ReviewImage();
                        reviewImage.setReview(savedReview);
                        reviewImage.setImageUrl(imageUrl);
                        reviewImages.add(reviewImage);
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при загрузке изображения", e);
                    }
                }
            }
            imageRepository.saveAll(reviewImages);
        }

        return mapToDTO(savedReview);
    }

    /**
     * ТЗ: "Для всех пользователей – просмотр оценок и комментариев... фильтрация комментариев по оценке"
     */
    public List<ReviewDTO> getReviewsByProductId(Long productId, Integer filterRating) {
        List<Review> reviews;

        if (filterRating != null) {
            // Фильтрация по рейтингу + только не удаленные
            reviews = reviewRepository.findByProductIdAndRatingAndIsDeletedFalseOrderByCreatedAtDesc(productId, filterRating);
        } else {
            // Все активные отзывы
            reviews = reviewRepository.findByProductIdAndIsDeletedFalseOrderByCreatedAtDesc(productId);
        }

        return reviews.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    /**
     * ТЗ: "Для администрации – возможность удаления комментариев"
     * Мягкое удаление (флаг isDeleted)
     */
    @Transactional
    public void deleteReview(Long reviewId, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Отзыв не найден"));

        if (isAdmin) {
            // Админ может удалить любой
            review.setIsDeleted(true);
            reviewRepository.save(review);
        } else {
            throw new IllegalStateException("Удаление доступно только администратору");
        }
    }

    /**
     * Удаление конкретного изображения из отзыва (для админа)
     */
    @Transactional
    public void deleteImage(Long imageId) {
        ReviewImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Изображение не найдено"));

        // Можно удалить физический файл здесь, если нужно
        imageRepository.delete(image);
    }

    /**
     * Вспомогательный метод для сохранения файла
     */
    private String saveImage(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR + fileName);

        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());

        return "/uploads/reviews/" + fileName; // Возвращаем URL путь
    }

    /**
     * Маппинг Entity -> DTO
     */
    private ReviewDTO mapToDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setProductId(review.getProduct().getId());
        dto.setUserName(review.getUser().getFirstName() + " " + review.getUser().getLastName());
        dto.setRating(review.getRating());
        dto.setCommentText(review.getCommentText());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setDeleted(review.getIsDeleted());

        if (review.getImages() != null) {
            List<ReviewImageDTO> imageDtos = review.getImages().stream()
                    .map(img -> {
                        ReviewImageDTO iDto = new ReviewImageDTO();
                        iDto.setId(img.getId());
                        iDto.setImageUrl(img.getImageUrl());
                        return iDto;
                    })
                    .collect(Collectors.toList());
            dto.setImages(imageDtos);
        }

        return dto;
    }
}