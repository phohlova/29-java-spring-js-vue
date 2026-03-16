package org.example._9javaspringjsvue.repository;

import jakarta.transaction.Transactional;
import org.example._9javaspringjsvue.entity.Product;
import org.example._9javaspringjsvue.entity.ProductAttribute;
import org.example._9javaspringjsvue.entity.ProductAttributeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, ProductAttributeId> {
    // Найти все характеристики товара
    List<ProductAttribute> findByProduct(Product product);

    // Удалить все характеристики товара
    @Modifying
    @Transactional
    void deleteByProduct(Product product);

    // Найти товары по значению характеристики
    @Query("SELECT pa.product FROM ProductAttribute pa " +
            "JOIN pa.attribute a " +
            "WHERE a.name = :attributeName AND pa.value = :value")
    List<Product> findProductsByAttributeValue(@Param("attributeName") String attributeName,
                                               @Param("value") String value);

    List<ProductAttribute> findByAttributeNameAndValue(String attributeName, String value);
}
