package org.example._9javaspringjsvue.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CategoryWithBreadcrumbsDTO {
    private Long id;
    private String name;
    private String slug;
    private List<BreadcrumbItem> breadcrumbs = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class BreadcrumbItem {
        private Long id;
        private String name;
        private String slug;

        public BreadcrumbItem(Long id, String name, String slug) {
            this.id = id;
            this.name = name;
            this.slug = slug;
        }
    }
}