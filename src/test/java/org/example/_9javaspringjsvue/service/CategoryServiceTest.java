package org.example._9javaspringjsvue.service;

import org.example._9javaspringjsvue.dto.CategoryDTO;
import org.example._9javaspringjsvue.dto.CategoryWithBreadcrumbsDTO;
import org.example._9javaspringjsvue.entity.Category;
import org.example._9javaspringjsvue.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category rootCategory;
    private Category childCategory;
    private Category grandchildCategory;

    @BeforeEach
    void setUp() {
        rootCategory = new Category();
        rootCategory.setId(1L);
        rootCategory.setName("Смартфоны");
        rootCategory.setSlug("smartphones");
        rootCategory.setParent(null);
        rootCategory.setSortOrder(1);

        childCategory = new Category();
        childCategory.setId(2L);
        childCategory.setName("Apple");
        childCategory.setSlug("apple");
        childCategory.setParent(rootCategory);
        childCategory.setSortOrder(1);

        grandchildCategory = new Category();
        grandchildCategory.setId(3L);
        grandchildCategory.setName("iPhone");
        grandchildCategory.setSlug("iphone");
        grandchildCategory.setParent(childCategory);
        grandchildCategory.setSortOrder(1);

        rootCategory.setChildren(Arrays.asList(childCategory));
        childCategory.setChildren(Arrays.asList(grandchildCategory));
        grandchildCategory.setChildren(Collections.emptyList());
    }

    @Test
    void getCategoryWithBreadcrumbs_ShouldReturnPathFromRootToCategory() {
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(grandchildCategory));

        CategoryWithBreadcrumbsDTO result = categoryService.getCategoryWithBreadcrumbs(3L);

        assertNotNull(result);
        assertEquals("iPhone", result.getName());

        List<CategoryWithBreadcrumbsDTO.BreadcrumbItem> breadcrumbs = result.getBreadcrumbs();
        assertNotNull(breadcrumbs);
        assertEquals(3, breadcrumbs.size()); // Смартфоны -> Apple -> iPhone

        assertEquals("Смартфоны", breadcrumbs.get(0).getName());
        assertEquals("Apple", breadcrumbs.get(1).getName());
        assertEquals("iPhone", breadcrumbs.get(2).getName());
    }

    @Test
    void getCategoryWithBreadcrumbs_ShouldThrowException_IfCategoryNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            categoryService.getCategoryWithBreadcrumbs(999L);
        });
        assertEquals("Категория не найдена", exception.getMessage());
    }

    @Test
    void getProductsCountInCategory_ShouldReturnCountFromRepository() {
        long expectedCount = 15L;
        when(categoryRepository.countProductsInCategoryWithSubcategories(1L))
                .thenReturn(expectedCount);

        Long result = categoryService.getProductsCountInCategory(1L);

        assertEquals(expectedCount, result);
        verify(categoryRepository, times(1)).countProductsInCategoryWithSubcategories(1L);
    }

    @Test
    void getCategoryTree_ShouldReturnRootCategoriesWithChildren() {
        List<Category> roots = Collections.singletonList(rootCategory);
        when(categoryRepository.findByParentIsNullOrderBySortOrder()).thenReturn(roots);

        when(categoryRepository.countProductsInCategoryWithSubcategories(1L)).thenReturn(10L);
        when(categoryRepository.countProductsInCategoryWithSubcategories(2L)).thenReturn(8L);
        when(categoryRepository.countProductsInCategoryWithSubcategories(3L)).thenReturn(5L);

        List<CategoryDTO> result = categoryService.getCategoryTree();

        assertNotNull(result);
        assertEquals(1, result.size());

        CategoryDTO rootDto = result.get(0);
        assertEquals("Смартфоны", rootDto.getName());
        assertEquals(10L, rootDto.getProductCount());

        assertNotNull(rootDto.getChildren());
        assertEquals(1, rootDto.getChildren().size());

        CategoryDTO childDto = rootDto.getChildren().get(0);
        assertEquals("Apple", childDto.getName());
        assertEquals(8L, childDto.getProductCount());
    }

    @Test
    void getAllCategories_ShouldReturnFlatList() {
        List<Category> all = Arrays.asList(rootCategory, childCategory, grandchildCategory);
        when(categoryRepository.findAllByOrderBySortOrder()).thenReturn(all);

        List<CategoryDTO> result = categoryService.getAllCategories();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(0L, result.get(0).getProductCount());
    }
}