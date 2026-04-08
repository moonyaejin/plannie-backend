package com.plannie.adapter.out.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenAI 설정 프로퍼티
 * application.yml: openai.api-key, openai.model
 */
@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(String apiKey, String model) {}
