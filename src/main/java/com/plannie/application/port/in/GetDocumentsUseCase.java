package com.plannie.application.port.in;

import com.plannie.domain.document.Document;

import java.util.List;

public interface GetDocumentsUseCase {

    List<Document> getDocuments(Long userId);
}
