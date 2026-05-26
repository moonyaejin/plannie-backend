package com.plannie.adapter.out.ai;

import com.plannie.adapter.out.ai.dto.OpenAiEmbeddingRequest;
import com.plannie.adapter.out.ai.dto.OpenAiEmbeddingResponse;
import com.plannie.application.port.out.EmbedTextPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiEmbeddingAdapter implements EmbedTextPort {

    private final WebClient openAiWebClient;

    @Override
    @CircuitBreaker(name = "openai", fallbackMethod = "embedFallback")
    @Retry(name = "openai")
    public float[] embed(String text) {
        OpenAiEmbeddingRequest request = new OpenAiEmbeddingRequest(List.of(text));

        OpenAiEmbeddingResponse response = openAiWebClient.post()
                .uri("/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAiEmbeddingResponse.class)
                .block();

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }

        List<Float> floats = response.data().get(0).embedding();
        if (floats == null || floats.isEmpty()) {
            throw new BusinessException(ErrorCode.OPENAI_PARSE_ERROR);
        }
        float[] arr = new float[floats.size()];
        for (int i = 0; i < floats.size(); i++) arr[i] = floats.get(i);
        return arr;
    }

    private float[] embedFallback(String text, Throwable t) {
        if (t instanceof BusinessException e) throw e;
        if (t instanceof WebClientResponseException e) {
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR, "OpenAI API 오류: " + e.getStatusCode());
        }
        throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
    }

    @Override
    @CircuitBreaker(name = "openai", fallbackMethod = "embedBatchFallback")
    @Retry(name = "openai")
    public List<float[]> embedBatch(List<String> texts) {
        OpenAiEmbeddingRequest request = new OpenAiEmbeddingRequest(texts);

        OpenAiEmbeddingResponse response = openAiWebClient.post()
                .uri("/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAiEmbeddingResponse.class)
                .block();

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }

        return response.data().stream()
                .sorted((a, b) -> Integer.compare(a.index(), b.index()))
                .map(d -> {
                    List<Float> floats = d.embedding();
                    if (floats == null || floats.isEmpty()) {
                        throw new BusinessException(ErrorCode.OPENAI_PARSE_ERROR);
                    }
                    float[] arr = new float[floats.size()];
                    for (int i = 0; i < floats.size(); i++) arr[i] = floats.get(i);
                    return arr;
                })
                .toList();
    }

    private List<float[]> embedBatchFallback(List<String> texts, Throwable t) {
        if (t instanceof BusinessException e) throw e;
        if (t instanceof WebClientResponseException e) {
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR, "OpenAI API 오류: " + e.getStatusCode());
        }
        throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
    }
}
