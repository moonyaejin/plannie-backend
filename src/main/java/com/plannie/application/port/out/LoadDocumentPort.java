package com.plannie.application.port.out;

import com.plannie.domain.document.Document;

import java.util.List;
import java.util.Optional;

public interface LoadDocumentPort {

    List<Document> findByUserId(Long userId);

    Optional<Document> findByIdAndUserId(Long documentId, Long userId);
}
