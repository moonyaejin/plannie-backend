package com.plannie.application.port.out;

public interface DeleteDocumentPort {

    void deleteByIdAndUserId(Long documentId, Long userId);
}
