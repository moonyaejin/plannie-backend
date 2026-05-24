package com.plannie.application.port.in;

public interface DeleteDocumentUseCase {

    void delete(Long userId, Long documentId);
}
