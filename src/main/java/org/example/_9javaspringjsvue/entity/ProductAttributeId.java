package org.example._9javaspringjsvue.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class ProductAttributeId implements Serializable {
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "attribute_id")
    private Long attributeId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductAttributeId that = (ProductAttributeId) o;
        return productId.equals(that.productId) && attributeId.equals(that.attributeId);
    }

    @Override
    public int hashCode() {
        return 31 * productId.hashCode() + attributeId.hashCode();
    }
}
