package org.example._9javaspringjsvue.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private Double price;
    private Double basePrice;
    private Double discountPrice;
    private Integer stockQuantity;
    private String availabilityStatus;
    private Double averageRating;
    private List<CategoryDTO> categories;
    private List<ProductAttributeDTO> attributes;
}
