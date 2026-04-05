package org.example._9javaspringjsvue.repository;

import org.example._9javaspringjsvue.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Найти корневые категории (у которых нет родителя)
    List<Category> findByParentIsNullOrderBySortOrder();

    // Найти дочерние категории конкретного родителя
    List<Category> findByParentOrderBySortOrder(Category parent);

    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId ORDER BY c.sortOrder")
    List<Category> findByParentId(@Param("parentId") Long parentId);

    Optional<Category> findBySlug(String slug);
    List<Category> findAllByOrderBySortOrder();
    List<Category> findByNameContainingIgnoreCase(String name);

    @Query("SELECT COUNT(DISTINCT p) FROM Product p JOIN p.categories c WHERE c.id = :categoryId OR c.parent.id = :categoryId")
    Long countProductsInCategoryWithSubcategories(@Param("categoryId") Long categoryId);
}