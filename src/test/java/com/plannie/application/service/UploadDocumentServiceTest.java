package com.plannie.application.service;

import com.plannie.application.port.out.EmbedTextPort;
import com.plannie.application.port.out.SaveDocumentPort;
import com.plannie.domain.document.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@Tag("fast")
@ExtendWith(MockitoExtension.class)
class UploadDocumentServiceTest {

    @Mock private SaveDocumentPort saveDocumentPort;
    @Mock private EmbedTextPort embedTextPort;
    @InjectMocks private UploadDocumentService uploadDocumentService;

    private static final Long USER_ID = 1L;

    @Test
    @DisplayName("TXT 파일을 업로드하면 청킹 후 임베딩하여 저장한다")
    void TXT_업로드_성공() {
        String text = "A".repeat(600); // 500자 청크 2개 생성
        byte[] fileBytes = text.getBytes();
        Document saved = new Document(10L, USER_ID, "test.txt", "txt", java.time.LocalDateTime.now());

        given(saveDocumentPort.saveDocument(any())).willReturn(saved);
        given(embedTextPort.embedBatch(anyList())).willReturn(List.of(new float[1536], new float[1536]));

        Document result = uploadDocumentService.upload(
                new com.plannie.application.port.in.UploadDocumentUseCase.Command(USER_ID, "test.txt", "txt", fileBytes)
        );

        assertThat(result.getId()).isEqualTo(10L);
        then(embedTextPort).should().embedBatch(argThat(chunks -> chunks.size() == 2));
        then(saveDocumentPort).should().saveChunks(argThat(chunks -> chunks.size() == 2));
    }

    @Test
    @DisplayName("짧은 텍스트는 청크 1개만 생성한다")
    void 짧은_텍스트_단일_청크() {
        String text = "짧은 내용";
        byte[] fileBytes = text.getBytes();
        Document saved = new Document(11L, USER_ID, "short.txt", "txt", java.time.LocalDateTime.now());

        given(saveDocumentPort.saveDocument(any())).willReturn(saved);
        given(embedTextPort.embedBatch(anyList())).willReturn(List.of(new float[1536]));

        uploadDocumentService.upload(
                new com.plannie.application.port.in.UploadDocumentUseCase.Command(USER_ID, "short.txt", "txt", fileBytes)
        );

        then(embedTextPort).should().embedBatch(argThat(chunks -> chunks.size() == 1));
    }

    @Test
    @DisplayName("빈 파일은 청크가 없어 임베딩하지 않는다")
    void 빈_파일_청크_없음() {
        byte[] fileBytes = "   ".getBytes();
        Document saved = new Document(12L, USER_ID, "empty.txt", "txt", java.time.LocalDateTime.now());

        given(saveDocumentPort.saveDocument(any())).willReturn(saved);
        given(embedTextPort.embedBatch(List.of())).willReturn(List.of());

        uploadDocumentService.upload(
                new com.plannie.application.port.in.UploadDocumentUseCase.Command(USER_ID, "empty.txt", "txt", fileBytes)
        );

        then(saveDocumentPort).should().saveChunks(argThat(List::isEmpty));
    }

    @Test
    @DisplayName("chunk() 메서드는 CHUNK_SIZE 단위로 분할한다")
    void 청킹_단위_검증() {
        String text = "X".repeat(1100); // 500 + 450 + (450-50=400) ... 실제로는 500/450/400 = 3청크
        List<String> chunks = uploadDocumentService.chunk(text);

        assertThat(chunks).isNotEmpty();
        chunks.forEach(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(500));
    }
}
