package org.example._9javaspringjsvue.controller;

import org.example._9javaspringjsvue.dto.CategoryDTO;
import org.example._9javaspringjsvue.dto.CategoryWithBreadcrumbsDTO;
import org.example._9javaspringjsvue.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * ТЗ: "всплывающее меню со структурой каталога"
     * Возвращает дерево категорий (корневые + вложенные)
     * GET /api/categories/tree
     */
    @GetMapping("/tree")
    public ResponseEntity<List<CategoryDTO>> getCategoryTree() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }

    /**
     * Возвращает путь от корня до текущей категории
     * GET /api/categories/{id}/breadcrumbs
     */
    @GetMapping("/{id}/breadcrumbs")
    public ResponseEntity<CategoryWithBreadcrumbsDTO> getBreadcrumbs(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryWithBreadcrumbs(id));
    }

    /**
     * Получение всех категорий плоским списком (для админки или фильтров)
     * GET /api/categories
     */
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    /**
     * Получение категории по slug (для красивых URL)
     * GET /api/categories/slug/{slug}
     */
    @GetMapping("slug/{slug}")
    public ResponseEntity<CategoryDTO> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(categoryService.getCategoryBySlug(slug));
    }
}
