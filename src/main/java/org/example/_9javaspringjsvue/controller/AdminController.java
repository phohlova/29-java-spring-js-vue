package org.example._9javaspringjsvue.controller;

import org.example._9javaspringjsvue.entity.Category;
import org.example._9javaspringjsvue.entity.Product;
import org.example._9javaspringjsvue.repository.UserRepository;
import org.example._9javaspringjsvue.service.AdminCatalogService;
import org.example._9javaspringjsvue.service.CategoryService;
import org.example._9javaspringjsvue.service.ProductService;
import org.example._9javaspringjsvue.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminCatalogService adminCatalogService;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final ReviewService reviewService;
    private final UserRepository userRepository;

    public AdminController(AdminCatalogService adminCatalogService, CategoryService categoryService, ProductService productService, ReviewService reviewService, UserRepository userRepository) {
        this.adminCatalogService = adminCatalogService;
        this.categoryService = categoryService;
        this.productService = productService;
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    /**
     * ТЗ: добавление категории
     */
    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> createCategory(@RequestParam String name,
                                                   @RequestParam String slug,
                                                   @RequestParam(required = false) Integer sortOrder,
                                                   @RequestParam(required = false) Long parentId) {
        return ResponseEntity.ok(adminCatalogService.createCategory(name, slug, sortOrder, parentId));
    }

    /**
     * ТЗ: редактирование / перемещение категории
     */
    @PutMapping("categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id,
                                                   @RequestParam String name,
                                                   @RequestParam String slug,
                                                   @RequestParam(required = false) Integer sortOrder,
                                                   @RequestParam(required = false) Long parentId) {
        return ResponseEntity.ok(adminCatalogService.updateCategory(id, name, slug, sortOrder, parentId));
    }

    /**
     * ТЗ: удаление категории
     */
    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        adminCatalogService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }

    /**
     * ТЗ: добавление товара
     * Поддержка загрузки изображения
     */
    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> createProduct(@RequestParam String title,
                                                 @RequestParam String description,
                                                 @RequestParam Double basePrice,
                                                 @RequestParam(required = false) Double discountPrice,
                                                 @RequestParam Integer stockQuantity,
                                                 @RequestParam List<Long> categoryIds,
                                                 @RequestParam(required = false) MultipartFile imageFile
    ) throws Exception {
        return ResponseEntity.ok(adminCatalogService.createProduct(title, description, null, basePrice, discountPrice, stockQuantity, categoryIds, imageFile));
    }

    /**
     * ТЗ: редактирование товара
     */
    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id,
                                                 @RequestParam String title,
                                                 @RequestParam String description,
                                                 @RequestParam Double basePrice,
                                                 @RequestParam(required = false) Double discountPrice,
                                                 @RequestParam Integer stockQuantity,
                                                 @RequestParam List<Long> categoryIds,
                                                 @RequestParam(required = false) MultipartFile imageFile) throws Exception {
        return ResponseEntity.ok(adminCatalogService.updateProduct(
                id, title, description, basePrice, discountPrice, stockQuantity, categoryIds, imageFile
        ));
    }

    /**
     * ТЗ: удаление товара
     */
    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        adminCatalogService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }

    /**
     * ТЗ: удаление комментариев
     */
    @DeleteMapping("/reviews/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id, true);
        return ResponseEntity.ok().build();
    }

    /**
     * ТЗ: удаление фото в комментариях
     */
    @DeleteMapping("/reviews/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReviewImage(@PathVariable Long imageId) {
        reviewService.deleteImage(imageId);
        return ResponseEntity.ok().build();
    }
}
