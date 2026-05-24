package com.plannie.domain.document;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Document {

    private final Long id;
    private final Long userId;
    private final String fileName;
    private final String fileType;
    private final LocalDateTime createdAt;

    public Document(Long id, Long userId, String fileName, String fileType, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.createdAt = createdAt;
    }

    public static Document create(Long userId, String fileName, String fileType) {
        return new Document(null, userId, fileName, fileType, LocalDateTime.now());
    }
}
