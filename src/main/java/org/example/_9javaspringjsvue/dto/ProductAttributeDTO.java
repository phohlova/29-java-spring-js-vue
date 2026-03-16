package org.example._9javaspringjsvue.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProductAttributeDTO {
    private Long id;
    private String attributeName;
    private String value;
}
