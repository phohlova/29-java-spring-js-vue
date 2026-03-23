package org.example._9javaspringjsvue.controller;

import org.example._9javaspringjsvue.dto.ProductDTO;
import org.example._9javaspringjsvue.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * ТЗ: листинг товаров с сортировкой и учетом прав доступа для цены
     * GET /api/products?categoryId=1&sort-price_asc
     */
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getProducts(@RequestParam Long categoryId,
                                                        @RequestParam(required = false) String sort,
                                                        Authentication authentication) {
        boolean isAuthorized = (authentication != null && authentication.isAuthenticated());

        return ResponseEntity.ok(productService.getProductsByCategory(categoryId, sort, isAuthorized));
    }

    /**
     * ТЗ: карточка товара
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable Long id, Authentication authentication) {
        boolean isAuthorized = (authentication != null && authentication.isAuthenticated());

        return ResponseEntity.ok(productService.getProductById(id, isAuthorized));
    }

    /**
     * ТЗ: фильтрация по оценке
     * GET /api/products/filter/rating?minRating=4.5
     */
    @GetMapping("/filter/rating")
    public ResponseEntity<List<ProductDTO>> getByRating(@RequestParam Double minRating,
                                                        Authentication authentication) {
        boolean isAuthorized = (authentication != null && authentication.isAuthenticated());

        return ResponseEntity.ok(productService.findByMinRating(minRating, isAuthorized));
    }

    /**
     * ТЗ: поиск по характеристикам
     * GET /api/products/filter/attribute?attrName=Цвет&attrValue=Черный
     */
    public ResponseEntity<List<ProductDTO>> getByAttribute(@RequestParam String attrName,
                                                           @RequestParam String attrValue,
                                                           Authentication authentication) {
        boolean isAuthorized = (authentication != null && authentication.isAuthenticated());

        return ResponseEntity.ok(productService.findByAttributes(attrName, attrValue, isAuthorized));
    }
}
