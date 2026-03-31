package org.example._9javaspringjsvue.service;

import org.example._9javaspringjsvue.dto.ReviewDTO;
import org.example._9javaspringjsvue.entity.Product;
import org.example._9javaspringjsvue.entity.Review;
import org.example._9javaspringjsvue.entity.ReviewImage;
import org.example._9javaspringjsvue.entity.User;
import org.example._9javaspringjsvue.repository.ProductRepository;
import org.example._9javaspringjsvue.repository.ReviewImageRepository;
import org.example._9javaspringjsvue.repository.ReviewRepository;
import org.example._9javaspringjsvue.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewImageRepository imageRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User testUser;
    private Product testProduct;
    private Review testReview;

    @BeforeEach
    void setUp() {
        // Пользователь
        testUser = new User();
        testUser.setId(10L);
        testUser.setEmail("user@example.com");
        testUser.setFirstName("Иван");
        testUser.setLastName("Иванов");

        // Товар
        testProduct = new Product();
        testProduct.setId(100L);
        testProduct.setTitle("iPhone 17");

        // Отзыв (шаблон)
        testReview = new Review();
        testReview.setId(500L);
        testReview.setUser(testUser);
        testReview.setProduct(testProduct);
        testReview.setRating(5);
        testReview.setCommentText("Отличный телефон!");
        testReview.setIsDeleted(false);
        testReview.setCreatedAt(LocalDateTime.now());
        testReview.setImages(Collections.emptyList());
    }

    @Test
    void createReview_ShouldSuccess_WithoutImages() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArguments()[0]);

        ReviewDTO result = reviewService.createReview(10L, 100L, 5, "Супер!", null);

        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals("Супер!", result.getCommentText());
        assertEquals("Иван Иванов", result.getUserName());
        assertTrue(result.getImages().isEmpty());

        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(imageRepository, never()).saveAll(anyList());
    }

    @Test
    void createReview_ShouldSuccess_WithImages() throws IOException {
        MultipartFile mockFile1 = new MockMultipartFile("file", "photo1.jpg", "image/jpeg", "binary_data_1".getBytes());
        MultipartFile mockFile2 = new MockMultipartFile("file", "photo2.png", "image/png", "binary_data_2".getBytes());
        List<MultipartFile> files = Arrays.asList(mockFile1, mockFile2);

        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> {
            Review r = (Review) i.getArguments()[0];
            r.setId(501L);
            return r;
        });
        when(imageRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        ReviewDTO result = reviewService.createReview(10L, 100L, 4, "Хорошо, но дорого", files);

        assertNotNull(result);
        assertEquals(4, result.getRating());
        verify(imageRepository, times(1)).saveAll(anyList());
    }

    @Test
    void createReview_ShouldThrowException_IfInvalidRating_Low() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            reviewService.createReview(10L, 100L, 0, "Текст", null);
        });
        assertEquals("Рейтинг должен быть от 1 до 5", exception.getMessage());
    }

    @Test
    void createReview_ShouldThrowException_IfInvalidRating_High() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            reviewService.createReview(10L, 100L, 6, "Текст", null);
        });
        assertEquals("Рейтинг должен быть от 1 до 5", exception.getMessage());
    }

    @Test
    void createReview_ShouldThrowException_IfUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reviewService.createReview(999L, 100L, 5, "Текст", null);
        });
        assertEquals("Пользователь не найден", exception.getMessage());
    }

    @Test
    void createReview_ShouldThrowException_IfProductNotFound() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reviewService.createReview(10L, 999L, 5, "Текст", null);
        });
        assertEquals("Товар не найден", exception.getMessage());
    }


    @Test
    void getReviewsByProductId_ShouldReturnAll_WhenNoFilter() {
        Review r1 = new Review();
        r1.setId(1L);
        r1.setRating(5);
        r1.setIsDeleted(false);
        r1.setProduct(testProduct);

        r1.setUser(testUser);

        Review r2 = new Review();
        r2.setId(2L);
        r2.setRating(3);
        r2.setIsDeleted(false);
        r2.setProduct(testProduct);
        r2.setUser(testUser);

        List<Review> allReviews = Arrays.asList(r1, r2);
        when(reviewRepository.findByProductIdAndIsDeletedFalseOrderByCreatedAtDesc(100L))
                .thenReturn(allReviews);

        List<ReviewDTO> result = reviewService.getReviewsByProductId(100L, null);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(reviewRepository, times(1)).findByProductIdAndIsDeletedFalseOrderByCreatedAtDesc(100L);
    }

    @Test
    void getReviewsByProductId_ShouldFilterByRating() {
        Review good = new Review();
        good.setId(1L);
        good.setRating(5);
        good.setIsDeleted(false);
        good.setProduct(testProduct);
        good.setUser(testUser);

        List<Review> filteredReviews = Collections.singletonList(good);

        when(reviewRepository.findByProductIdAndRatingAndIsDeletedFalseOrderByCreatedAtDesc(100L, 5))
                .thenReturn(filteredReviews);

        List<ReviewDTO> result = reviewService.getReviewsByProductId(100L, 5);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getRating());
    }


    @Test
    void deleteReview_ShouldSoftDelete_WhenAdmin() {
        when(reviewRepository.findById(500L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArguments()[0]);

        reviewService.deleteReview(500L, true); // true = админ

        assertTrue(testReview.getIsDeleted(), "Флаг isDeleted должен стать true");
        verify(reviewRepository, times(1)).save(testReview);
    }

    @Test
    void deleteReview_ShouldThrowException_WhenNotAdmin() {
        when(reviewRepository.findById(500L)).thenReturn(Optional.of(testReview));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            reviewService.deleteReview(500L, false); // false = не админ
        });
        assertEquals("Удаление доступно только администратору", exception.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void deleteImage_ShouldRemoveImage() {
        ReviewImage img = new ReviewImage();
        img.setId(99L);
        img.setImageUrl("/uploads/reviews/img.jpg");

        when(imageRepository.findById(99L)).thenReturn(Optional.of(img));

        reviewService.deleteImage(99L);

        verify(imageRepository, times(1)).delete(img);
    }
}