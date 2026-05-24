package com.plannie.application.service;

import com.plannie.application.port.in.AskDocumentUseCase.Answer;
import com.plannie.application.port.in.AskDocumentUseCase.Command;
import com.plannie.application.port.out.EmbedTextPort;
import com.plannie.application.port.out.GenerateRagAnswerPort;
import com.plannie.application.port.out.SearchSimilarChunksPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@Tag("fast")
@ExtendWith(MockitoExtension.class)
class AskDocumentServiceTest {

    @Mock private EmbedTextPort embedTextPort;
    @Mock private SearchSimilarChunksPort searchSimilarChunksPort;
    @Mock private GenerateRagAnswerPort generateRagAnswerPort;
    @InjectMocks private AskDocumentService askDocumentService;

    private static final Long USER_ID = 1L;
    private static final float[] DUMMY_EMBEDDING = new float[1536];

    @Test
    @DisplayName("관련 청크를 찾으면 GPT 답변을 반환한다")
    void 질문_성공() {
        given(embedTextPort.embed("미적분이란?")).willReturn(DUMMY_EMBEDDING);
        given(searchSimilarChunksPort.searchTopK(eq(USER_ID), any(), eq(5)))
                .willReturn(List.of("미적분은 변화율을 다루는 수학 분야입니다."));
        given(generateRagAnswerPort.generate(eq("미적분이란?"), anyList()))
                .willReturn("미적분은 변화와 누적을 수학적으로 다루는 학문입니다.");

        Answer answer = askDocumentService.ask(new Command(USER_ID, "미적분이란?"));

        assertThat(answer.question()).isEqualTo("미적분이란?");
        assertThat(answer.answer()).isEqualTo("미적분은 변화와 누적을 수학적으로 다루는 학문입니다.");
    }

    @Test
    @DisplayName("관련 청크가 없으면 DOCUMENT_NO_CONTEXT 예외를 던진다")
    void 관련_청크_없음_예외() {
        given(embedTextPort.embed(anyString())).willReturn(DUMMY_EMBEDDING);
        given(searchSimilarChunksPort.searchTopK(eq(USER_ID), any(), eq(5)))
                .willReturn(List.of());

        assertThatThrownBy(() -> askDocumentService.ask(new Command(USER_ID, "없는 내용")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DOCUMENT_NO_CONTEXT);
    }
}
