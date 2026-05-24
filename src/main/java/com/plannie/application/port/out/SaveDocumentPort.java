package com.plannie.application.port.out;

import com.plannie.domain.document.Document;
import com.plannie.domain.document.DocumentChunk;

import java.util.List;

public interface SaveDocumentPort {

    Document saveDocument(Document document);

    void saveChunks(List<DocumentChunk> chunks);
}
