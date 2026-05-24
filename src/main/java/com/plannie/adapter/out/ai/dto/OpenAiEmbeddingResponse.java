package com.plannie.adapter.out.ai.dto;

import java.util.List;

public record OpenAiEmbeddingResponse(List<EmbeddingData> data) {

    public record EmbeddingData(int index, List<Float> embedding) {}
}
