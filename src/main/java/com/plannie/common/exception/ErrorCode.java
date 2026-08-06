package com.plannie.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 오류가 발생했습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "요청한 API 경로를 찾을 수 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "U003", "비밀번호가 일치하지 않습니다."),

    // Schedule
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "일정을 찾을 수 없습니다."),
    SCHEDULE_CONFLICT(HttpStatus.CONFLICT, "S002", "해당 시간에 이미 일정이 있습니다."),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "S003", "시작 시간이 종료 시간보다 늦을 수 없습니다."),
    SCHEDULE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "S004", "해당 일정에 접근 권한이 없습니다."),
    RECURRING_SCHEDULE_TOGGLE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "S005", "반복 일정은 날짜를 지정하여 완료 처리해야 합니다."),
    SCHEDULE_TIME_PARTIALLY_SET(HttpStatus.BAD_REQUEST, "S006", "시작 시간과 종료 시간은 함께 입력하거나 함께 비워야 합니다."),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CT001", "카테고리를 찾을 수 없습니다."),
    CATEGORY_DUPLICATE(HttpStatus.CONFLICT, "CT002", "이미 같은 이름의 카테고리가 있습니다."),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "알림을 찾을 수 없습니다."),

    // StudySession
    STUDY_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SS003", "공부 세션을 찾을 수 없습니다."),
    STUDY_SESSION_ALREADY_ACTIVE(HttpStatus.CONFLICT, "SS004", "이미 진행 중인 세션이 있습니다."),
    STUDY_SESSION_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "SS005", "진행 중인 세션이 아닙니다."),

    // Document
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "문서를 찾을 수 없습니다."),
    DOCUMENT_PARSE_ERROR(HttpStatus.BAD_REQUEST, "D002", "문서 파싱에 실패했습니다."),
    DOCUMENT_NO_CONTEXT(HttpStatus.BAD_REQUEST, "D003", "질문과 관련된 내용을 문서에서 찾을 수 없습니다."),

    // External API
    OPENAI_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "E001", "AI 서비스 연동 중 오류가 발생했습니다."),
    OPENAI_PARSE_ERROR(HttpStatus.BAD_REQUEST, "E002", "자연어 파싱에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
