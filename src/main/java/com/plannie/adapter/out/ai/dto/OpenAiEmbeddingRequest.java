package com.plannie.adapter.out.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OpenAiEmbeddingRequest(
        List<String> input,
        String model,
        @JsonProperty("encoding_format") String encodingFormat
) {
    public OpenAiEmbeddingRequest(List<String> input) {
        this(input, "text-embedding-3-small", "float");
    }
}
