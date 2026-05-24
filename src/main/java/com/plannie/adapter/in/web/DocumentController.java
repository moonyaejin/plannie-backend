package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.DocumentAskRequest;
import com.plannie.adapter.in.web.dto.DocumentAskResponse;
import com.plannie.adapter.in.web.dto.DocumentListResponse;
import com.plannie.adapter.in.web.dto.DocumentUploadResponse;
import com.plannie.application.port.in.AskDocumentUseCase;
import com.plannie.application.port.in.DeleteDocumentUseCase;
import com.plannie.application.port.in.GetDocumentsUseCase;
import com.plannie.application.port.in.UploadDocumentUseCase;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Tag(name = "Document", description = "RAG 문서 Q&A API")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "txt");
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L; // 10MB

    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final GetDocumentsUseCase getDocumentsUseCase;
    private final DeleteDocumentUseCase deleteDocumentUseCase;
    private final AskDocumentUseCase askDocumentUseCase;

    @Operation(summary = "문서 업로드", description = "PDF 또는 TXT 파일을 업로드하여 RAG 인덱싱합니다")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentUploadResponse upload(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase()
                : "";

        if (!ALLOWED_TYPES.contains(ext)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "PDF 또는 TXT 파일만 업로드 가능합니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "파일 크기는 10MB 이하여야 합니다.");
        }

        return DocumentUploadResponse.from(
                uploadDocumentUseCase.upload(new UploadDocumentUseCase.Command(
                        userId, originalName, ext, file.getBytes()
                ))
        );
    }

    @Operation(summary = "문서 목록 조회", description = "업로드한 문서 목록을 반환합니다")
    @GetMapping
    public List<DocumentListResponse> getDocuments(@AuthenticationPrincipal Long userId) {
        return getDocumentsUseCase.getDocuments(userId).stream()
                .map(DocumentListResponse::from)
                .toList();
    }

    @Operation(summary = "문서 삭제", description = "문서와 관련 청크를 삭제합니다")
    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long documentId) {
        deleteDocumentUseCase.delete(userId, documentId);
    }

    @Operation(summary = "문서 질문", description = "업로드한 문서를 기반으로 AI가 질문에 답변합니다")
    @PostMapping("/ask")
    public DocumentAskResponse ask(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody DocumentAskRequest request) {

        AskDocumentUseCase.Answer answer = askDocumentUseCase.ask(
                new AskDocumentUseCase.Command(userId, request.question())
        );
        return new DocumentAskResponse(answer.question(), answer.answer());
    }
}
