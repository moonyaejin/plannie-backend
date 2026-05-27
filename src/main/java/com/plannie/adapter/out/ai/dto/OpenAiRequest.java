package com.plannie.adapter.out.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiRequest(
        String model,
        List<Message> messages,
        @JsonProperty("max_tokens") Integer maxTokens
) {
    public OpenAiRequest(String model, List<Message> messages) {
        this(model, messages, null);
    }

    public record Message(String role, String content) {}
}
