package com.plannie.application.service;

import com.plannie.adapter.out.ai.dto.OpenAiRequest;
import com.plannie.application.port.in.AiChatUseCase;
import com.plannie.application.port.out.AiChatWithGptPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiChatService implements AiChatUseCase {

    private final AiChatWithGptPort aiChatWithGptPort;

    @Override
    public String chat(String message, List<Map<String, String>> history) {
        List<OpenAiRequest.Message> mapped = toOpenAiMessages(history);
        return aiChatWithGptPort.chat(message, mapped);
    }

    @Override
    public ExtractedPlanParams extractPlanParams(String text, List<Map<String, String>> history) {
        String context = buildConversationContext(history, text);
        return aiChatWithGptPort.extractPlanParams(context);
    }

    private List<OpenAiRequest.Message> toOpenAiMessages(List<Map<String, String>> history) {
        if (history == null) return List.of();
        return history.stream()
                .filter(m -> m.get("sender") != null && m.get("text") != null)
                .map(m -> new OpenAiRequest.Message(
                        "bot".equals(m.get("sender")) ? "assistant" : "user",
                        m.get("text")
                ))
                .toList();
    }

    private String buildConversationContext(List<Map<String, String>> history, String currentText) {
        StringBuilder sb = new StringBuilder();
        if (history != null) {
            for (Map<String, String> m : history) {
                String role = "bot".equals(m.get("sender")) ? "AI" : "사용자";
                sb.append(role).append(": ").append(m.get("text")).append("\n");
            }
        }
        sb.append("사용자: ").append(currentText);
        return sb.toString();
    }
}
