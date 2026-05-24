package com.plannie.application.port.in;

import com.plannie.domain.document.Document;

public interface UploadDocumentUseCase {

    record Command(Long userId, String fileName, String fileType, byte[] fileBytes) {}

    Document upload(Command command);
}
