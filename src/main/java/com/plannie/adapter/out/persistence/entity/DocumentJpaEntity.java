package com.plannie.adapter.out.persistence.entity;

import com.plannie.domain.document.Document;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor
public class DocumentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 10)
    private String fileType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static DocumentJpaEntity from(Document document) {
        DocumentJpaEntity entity = new DocumentJpaEntity();
        entity.userId = document.getUserId();
        entity.fileName = document.getFileName();
        entity.fileType = document.getFileType();
        entity.createdAt = document.getCreatedAt();
        return entity;
    }

    public Document toDomain() {
        return new Document(id, userId, fileName, fileType, createdAt);
    }
}
