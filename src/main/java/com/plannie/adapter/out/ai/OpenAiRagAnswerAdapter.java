package com.plannie.adapter.out.ai;

import com.plannie.adapter.out.ai.config.OpenAiProperties;
import com.plannie.adapter.out.ai.dto.OpenAiRequest;
import com.plannie.adapter.out.ai.dto.OpenAiResponse;
import com.plannie.application.port.out.GenerateRagAnswerPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OpenAiRagAnswerAdapter implements GenerateRagAnswerPort {

    private final WebClient openAiWebClient;
    private final OpenAiProperties openAiProperties;

    @Override
    @CircuitBreaker(name = "openai", fallbackMethod = "generateFallback")
    @Retry(name = "openai")
    public String generate(String question, List<String> contextChunks) {
        String context = contextChunks.stream()
                .collect(Collectors.joining("\n\n---\n\n"));

        OpenAiRequest request = new OpenAiRequest(
                openAiProperties.model(),
                List.of(
                        new OpenAiRequest.Message("system", buildSystemPrompt()),
                        new OpenAiRequest.Message("user", buildUserMessage(question, context))
                )
        );

        OpenAiResponse response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .block();

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }

        return response.choices().get(0).message().content();
    }

    private String generateFallback(String question, List<String> contextChunks, Throwable t) {
        if (t instanceof BusinessException e) throw e;
        if (t instanceof WebClientResponseException e) {
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR, "OpenAI API 오류: " + e.getStatusCode());
        }
        throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
    }

    private String buildSystemPrompt() {
        return """
                You are a helpful study assistant for a Korean student.
                Answer questions based ONLY on the provided document context.
                If the answer cannot be found in the context, say so clearly in Korean.
                Always respond in Korean.
                """;
    }

    private String buildUserMessage(String question, String context) {
        return """
                [문서 내용]
                %s

                [질문]
                %s
                """.formatted(context, question);
    }
}
