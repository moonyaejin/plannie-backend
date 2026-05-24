package com.plannie.domain.document;

import lombok.Getter;

@Getter
public class DocumentChunk {

    private final Long documentId;
    private final int chunkIndex;
    private final String content;
    private final float[] embedding;

    public DocumentChunk(Long documentId, int chunkIndex, String content, float[] embedding) {
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.embedding = embedding;
    }
}
