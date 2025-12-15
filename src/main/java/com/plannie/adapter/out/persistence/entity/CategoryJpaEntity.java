package com.plannie.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_category_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "category_name", nullable = false, length = 10)
    private String categoryName;

    @Column(name = "color", length = 200)
    private String color;

    @Builder
    public CategoryJpaEntity(Long id, Long userId, String categoryName, String color) {
        this.id = id;
        this.userId = userId;
        this.categoryName = categoryName;
        this.color = color;
    }
}
