package com.plannie.adapter.in.web.dto;

public record StudySessionRequest(
        Long categoryId,  // scheduleId 없을 때 필수 (카테고리 직접 타이머)
        Long scheduleId   // 있으면 categoryId는 무시되고 해당 일정의 카테고리로 자동 설정됨
) {}
