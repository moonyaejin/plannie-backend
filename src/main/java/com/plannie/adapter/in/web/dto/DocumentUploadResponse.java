package com.plannie.adapter.in.web.dto;

import com.plannie.domain.document.Document;

import java.time.LocalDateTime;

public record DocumentUploadResponse(
        Long id,
        String fileName,
        String fileType,
        LocalDateTime createdAt
) {
    public static DocumentUploadResponse from(Document document) {
        return new DocumentUploadResponse(
                document.getId(),
                document.getFileName(),
                document.getFileType(),
                document.getCreatedAt()
        );
    }
}
