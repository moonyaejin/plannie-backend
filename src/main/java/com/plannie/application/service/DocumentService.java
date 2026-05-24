package com.plannie.application.service;

import com.plannie.application.port.in.DeleteDocumentUseCase;
import com.plannie.application.port.in.GetDocumentsUseCase;
import com.plannie.application.port.out.DeleteDocumentPort;
import com.plannie.application.port.out.LoadDocumentPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.document.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService implements GetDocumentsUseCase, DeleteDocumentUseCase {

    private final LoadDocumentPort loadDocumentPort;
    private final DeleteDocumentPort deleteDocumentPort;

    @Override
    public List<Document> getDocuments(Long userId) {
        return loadDocumentPort.findByUserId(userId);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long documentId) {
        loadDocumentPort.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
        deleteDocumentPort.deleteByIdAndUserId(documentId, userId);
    }
}
