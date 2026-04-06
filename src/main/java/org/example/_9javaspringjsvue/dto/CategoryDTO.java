package org.example._9javaspringjsvue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Long id;
    private String name;
    private String slug;
    private Integer sortOrder;
    private Long productCount;
    private List<CategoryDTO> children = new ArrayList<>();
}
