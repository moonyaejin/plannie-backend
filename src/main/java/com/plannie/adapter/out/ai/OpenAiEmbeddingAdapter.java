package com.plannie.adapter.out.ai;

import com.plannie.adapter.out.ai.dto.OpenAiEmbeddingRequest;
import com.plannie.adapter.out.ai.dto.OpenAiEmbeddingResponse;
import com.plannie.application.port.out.EmbedTextPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OpenAiEmbeddingAdapter implements EmbedTextPort {

    private final WebClient openAiWebClient;

    @Override
    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
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
}
