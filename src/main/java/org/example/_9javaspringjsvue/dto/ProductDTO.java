package org.example._9javaspringjsvue.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;  // ✅ Импортируйте BigDecimal
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal basePrice;
    private BigDecimal discountPrice;
    private Integer stockQuantity;
    private String availabilityStatus;
    private Double averageRating;
    private List<CategoryDTO> categories;
    private List<ProductAttributeDTO> attributes;
}
