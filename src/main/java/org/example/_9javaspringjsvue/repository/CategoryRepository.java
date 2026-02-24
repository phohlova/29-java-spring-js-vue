package org.example._9javaspringjsvue.repository;

import org.example._9javaspringjsvue.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // всплывающее меню со структурой каталога
    List<Category> findByParentIsNull();

    // Найти подкатегории
    List<Category> findByParent(Category parent);

    // Найти категорию по slug
    Optional<Category> findBySlug(String slug);

    // Проверить, есть ли подкатегории
    boolean existsByParent(Category parent);

    // Найти все категории для drag-n-drop
    @Query("SELECT c FROM Category c ORDER BY c.sortOrder")
    List<Category> findAllOrdered();
}
