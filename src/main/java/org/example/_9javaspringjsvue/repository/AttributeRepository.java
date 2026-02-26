package org.example._9javaspringjsvue.repository;

import org.example._9javaspringjsvue.entity.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttributeRepository extends JpaRepository<Attribute, Long> {
    // Найти характеристику по имени
    Optional<Attribute> findByName(String name);

    // Проверить существование характеристики
    boolean existsByName(String name);

    // Найти все характеристики для фильтров
    List<Attribute> findAllByOrderByValueType();
}
