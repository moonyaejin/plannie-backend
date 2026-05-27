package com.plannie.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AiChatUseCase {
    String chat(String message, List<Map<String, String>> history);

    ExtractedPlanParams extractPlanParams(String text, List<Map<String, String>> history);

    record ExtractedPlanParams(
            String examName,
            LocalDate examDate,
            int dailyHours,
            List<String> focusAreas,
            String userRequest
    ) {}
}
