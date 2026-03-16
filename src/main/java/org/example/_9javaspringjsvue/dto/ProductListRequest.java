package org.example._9javaspringjsvue.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ProductListRequest {
    private Long categoryId;
    private String sort;
    private Double minRating;
    private Map<String, String> attributes;
    private Integer page;
    private Integer size;
}