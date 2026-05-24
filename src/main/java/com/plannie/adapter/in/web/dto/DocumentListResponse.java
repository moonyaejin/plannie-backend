package com.plannie.adapter.in.web.dto;

import com.plannie.domain.document.Document;

import java.time.LocalDateTime;

public record DocumentListResponse(
        Long id,
        String fileName,
        String fileType,
        LocalDateTime createdAt
) {
    public static DocumentListResponse from(Document document) {
        return new DocumentListResponse(
                document.getId(),
                document.getFileName(),
                document.getFileType(),
                document.getCreatedAt()
        );
    }
}
