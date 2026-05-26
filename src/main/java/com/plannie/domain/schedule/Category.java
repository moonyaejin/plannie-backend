package com.plannie.domain.schedule;

import lombok.Builder;
import lombok.Getter;

/**
 * 일정 카테고리 도메인 모델
 */
@Getter
public class Category {

    private final Long id;
    private final Long userId;
    private String name;
    private String color;  // HEX 코드 (예: #FF5733)

    @Builder
    public Category(Long id, Long userId, String name, String color) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.color = color;
    }

    public void update(String name, String color) {
        this.name = name;
        this.color = color;
    }

}
