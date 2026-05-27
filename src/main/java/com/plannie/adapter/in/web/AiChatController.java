package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.StudyPlanResponse;
import com.plannie.application.port.in.AiChatUseCase;
import com.plannie.application.port.in.AiChatUseCase.ExtractedPlanParams;
import com.plannie.application.port.in.GenerateStudyPlanUseCase;
import com.plannie.application.port.in.GenerateStudyPlanUseCase.GenerateStudyPlanCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatUseCase aiChatUseCase;
    private final GenerateStudyPlanUseCase generateStudyPlanUseCase;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String reply = aiChatUseCase.chat(request.text(), request.history());
        return ResponseEntity.ok(new ChatResponse(reply));
    }

    @PostMapping("/create-from-context")
    public ResponseEntity<StudyPlanResponse> createFromContext(
            @AuthenticationPrincipal Long userId,
            @RequestBody ContextCreateRequest request) {

        ExtractedPlanParams params = aiChatUseCase.extractPlanParams(request.text(), request.history());
        if (params == null) {
            return ResponseEntity.unprocessableEntity().build();
        }

        GenerateStudyPlanCommand command = new GenerateStudyPlanCommand(
                userId,
                params.examName(),
                LocalDate.now(),
                params.examDate(),
                null,
                params.dailyHours(),
                0,
                params.focusAreas(),
                params.userRequest()
        );

        return ResponseEntity.ok(StudyPlanResponse.from(generateStudyPlanUseCase.generate(command)));
    }

    record ChatRequest(@NotBlank String text, List<Map<String, String>> history) {}
    record ChatResponse(String reply) {}
    record ContextCreateRequest(String text, List<Map<String, String>> history) {}
}
