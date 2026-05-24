package com.plannie.application.service;

import com.plannie.application.port.in.UploadDocumentUseCase;
import com.plannie.application.port.out.EmbedTextPort;
import com.plannie.application.port.out.SaveDocumentPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import com.plannie.domain.document.Document;
import com.plannie.domain.document.DocumentChunk;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UploadDocumentService implements UploadDocumentUseCase {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    private final SaveDocumentPort saveDocumentPort;
    private final EmbedTextPort embedTextPort;

    @Override
    @Transactional
    public Document upload(Command command) {
        String text = extractText(command);
        List<String> chunkTexts = chunk(text);

        Document saved = saveDocumentPort.saveDocument(
                Document.create(command.userId(), command.fileName(), command.fileType())
        );

        List<float[]> embeddings = embedTextPort.embedBatch(chunkTexts);

        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < chunkTexts.size(); i++) {
            chunks.add(new DocumentChunk(saved.getId(), i, chunkTexts.get(i), embeddings.get(i)));
        }
        saveDocumentPort.saveChunks(chunks);

        return saved;
    }

    private String extractText(Command command) {
        try {
            if ("pdf".equalsIgnoreCase(command.fileType())) {
                try (PDDocument doc = Loader.loadPDF(command.fileBytes())) {
                    return new PDFTextStripper().getText(doc);
                }
            }
            return new String(command.fileBytes());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.DOCUMENT_PARSE_ERROR);
        }
    }

    List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            chunks.add(text.substring(start, end).strip());
            start += CHUNK_SIZE - CHUNK_OVERLAP;
        }
        chunks.removeIf(String::isBlank);
        return chunks;
    }
}
