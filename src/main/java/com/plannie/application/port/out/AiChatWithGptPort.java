package com.plannie.application.port.out;

import com.plannie.adapter.out.ai.dto.OpenAiRequest;
import com.plannie.application.port.in.AiChatUseCase.ExtractedPlanParams;
import java.util.List;

public interface AiChatWithGptPort {
    String chat(String userMessage, List<OpenAiRequest.Message> history);

    ExtractedPlanParams extractPlanParams(String conversationContext);
}
