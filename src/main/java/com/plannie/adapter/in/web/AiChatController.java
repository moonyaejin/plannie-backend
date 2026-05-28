package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.StudyPlanResponse;
import com.plannie.application.port.in.AiChatUseCase;
import com.plannie.application.port.in.AiChatUseCase.ExtractedPlanParams;
import com.plannie.application.port.in.GenerateStudyPlanUseCase;
import com.plannie.application.port.in.GenerateStudyPlanUseCase.GenerateStudyPlanCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatUseCase aiChatUseCase;
    private final GenerateStudyPlanUseCase generateStudyPlanUseCase;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChatRequest request) {

        String reply = aiChatUseCase.chat(request.text(), request.history());

        if (userId != null && reply.contains("\"action\": \"GENERATE_PLAN\"")) {
            String displayText = reply
                    .replaceAll("(?s)\\s*\\{\\s*\"action\"\\s*:\\s*\"GENERATE_PLAN\"\\s*\\}", "")
                    .trim();

            // 안전장치: 가장 마지막 봇 메시지가 "이대로 생성할까요?"일 때만 생성
            // (이전 계획의 오래된 제안 메시지가 히스토리에 남아 오탐하는 것을 방지)
            boolean hasPriorProposal = false;
            if (request.history() != null) {
                hasPriorProposal = request.history().stream()
                        .filter(m -> "bot".equals(m.get("sender")))
                        .reduce((a, b) -> b)
                        .map(m -> m.getOrDefault("text", "").contains("이대로 생성할까요?"))
                        .orElse(false);
            }

            if (!hasPriorProposal) {
                log.warn("GENERATE_PLAN 신호를 받았으나 사전 제안 없음 - 생성 차단");
                return ResponseEntity.ok(new ChatResponse(displayText, null));
            }

            try {
                ExtractedPlanParams params = aiChatUseCase.extractPlanParams(request.text(), request.history());
                if (params != null) {
                    GenerateStudyPlanCommand command = new GenerateStudyPlanCommand(
                            userId, params.examName(), LocalDate.now(), params.examDate(),
                            null, params.dailyHours(), 0, params.focusAreas(), params.userRequest());
                    StudyPlanResponse plan = StudyPlanResponse.from(generateStudyPlanUseCase.generate(command));
                    return ResponseEntity.ok(new ChatResponse(displayText, plan));
                }
            } catch (Exception e) {
                log.warn("GENERATE_PLAN 트리거 실패: {}", e.getMessage());
            }
            return ResponseEntity.ok(new ChatResponse(displayText, null));
        }

        return ResponseEntity.ok(new ChatResponse(reply, null));
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
    record ChatResponse(String reply, StudyPlanResponse plan) {}
    record ContextCreateRequest(String text, List<Map<String, String>> history) {}
}
