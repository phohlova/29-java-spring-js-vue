package org.example._9javaspringjsvue.service;

import org.example._9javaspringjsvue.dto.CategoryDTO;
import org.example._9javaspringjsvue.dto.CategoryWithBreadcrumbsDTO;
import org.example._9javaspringjsvue.entity.Category;
import org.example._9javaspringjsvue.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * ТЗ: "всплывающее меню со структурой каталога"
     * Возвращает дерево категорий (только корневые с вложенными)
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategoryTree() {
        List<Category> rootCategories = categoryRepository.findByParentIsNullOrderBySortOrder();

        return rootCategories.stream()
                .map(this::buildCategoryTree)
                .collect(Collectors.toList());
    }

    /**
     * ТЗ: "хлебные крошки" (breadcrumbs)
     * Возвращает путь от корневой категории до текущей
     * Пример: Главная / Смартфоны / Apple
     */
    @Transactional(readOnly = true)
    public CategoryWithBreadcrumbsDTO getCategoryWithBreadcrumbs(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Категория не найдена"));

        CategoryWithBreadcrumbsDTO dto = new CategoryWithBreadcrumbsDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setSlug(category.getSlug());

        List<CategoryWithBreadcrumbsDTO.BreadcrumbItem> breadcrumbs = new ArrayList<>();
        buildBreadcrumbs(category, breadcrumbs);

        java.util.Collections.reverse(breadcrumbs);
        dto.setBreadcrumbs(breadcrumbs);

        return dto;
    }

    /**
     * ТЗ: "количество товаров в данной категории и всех подкатегорий"
     * Возвращает количество товаров в категории
     */
    @Transactional(readOnly = true)
    public Long getProductsCountInCategory(Long categoryId) {
        // Используем запрос из Repository для подсчёта
        return categoryRepository.countProductsInCategoryWithSubcategories(categoryId);
    }

    /**
     * Получение всех категорий (для админки)
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        List<Category> allCategories = categoryRepository.findAllByOrderBySortOrder();
        return allCategories.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Получение категории по slug
     */
    @Transactional(readOnly = true)
    public CategoryDTO getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Категория не найдена"));

        return buildCategoryTree(category);
    }

    /**
     * Получение подкатегорий родителя
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getChildCategories(Long parentId) {
        List<Category> children = categoryRepository.findByParentId(parentId);
        return children.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }



    /**
     * Рекурсивно строит дерево категорий
     */
    private CategoryDTO buildCategoryTree(Category category) {
        CategoryDTO dto = mapToDTO(category);

        // Рекурсивно добавляем дочерние категории
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            List<CategoryDTO> childrenDTOs = category.getChildren().stream()
                    .map(this::buildCategoryTree)
                    .collect(Collectors.toList());
            dto.setChildren(childrenDTOs);
        }

        // Считаем количество товаров в категории и всех подкатегориях
        Long productCount = getProductsCountInCategory(category.getId());
        dto.setProductCount(productCount);

        return dto;
    }

    /**
     * Рекурсивно строит список хлебных крошек
     */
    private void buildBreadcrumbs(Category category,
                                  List<CategoryWithBreadcrumbsDTO.BreadcrumbItem> breadcrumbs) {
        breadcrumbs.add(new CategoryWithBreadcrumbsDTO.BreadcrumbItem(
                category.getId(),
                category.getName(),
                category.getSlug()
        ));

        // Если есть родитель, добавляем его в цепочку
        if (category.getParent() != null) {
            buildBreadcrumbs(category.getParent(), breadcrumbs);
        }
    }

    /**
     * Маппинг Entity → DTO (без дерева)
     */
    private CategoryDTO mapToDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setSlug(category.getSlug());
        dto.setSortOrder(category.getSortOrder());
        dto.setProductCount(0L);
        return dto;
    }
}
