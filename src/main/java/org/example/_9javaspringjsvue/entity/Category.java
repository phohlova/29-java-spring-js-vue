package org.example._9javaspringjsvue.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, length = 500)
    private String slug;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
