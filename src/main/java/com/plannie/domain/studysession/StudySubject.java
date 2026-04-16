package com.plannie.domain.studysession;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudySubject {

    private final Long id;
    private final Long userId;
    private String name;   // "수학", "영어", "운동" 등
    private String color;  // 프론트 UI용 색상 코드 (예: "#FF5733")

    public void update(String name, String color) {
        this.name = name;
        this.color = color;
    }
}
