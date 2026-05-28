package com.plannie.adapter.out.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plannie.adapter.out.ai.config.OpenAiProperties;
import com.plannie.adapter.out.ai.dto.GptStudyPlan;
import com.plannie.adapter.out.ai.dto.OpenAiRequest;
import com.plannie.adapter.out.ai.dto.OpenAiResponse;
import com.plannie.application.port.in.GenerateStudyPlanUseCase.GenerateStudyPlanCommand;
import com.plannie.application.port.out.GenerateStudyPlanWithAiPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiStudyPlanAdapter implements GenerateStudyPlanWithAiPort {

  private final WebClient openAiWebClient;
  private final OpenAiProperties openAiProperties;
  private final ObjectMapper objectMapper;

  @Override
  @CircuitBreaker(name = "openai", fallbackMethod = "generateFallback")
  @Retry(name = "openai")
  public AiStudyPlan generate(GenerateStudyPlanCommand command) {
    OpenAiRequest request = buildRequest(command);

    OpenAiResponse response = openAiWebClient.post()
        .uri("/chat/completions")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(OpenAiResponse.class)
        .block();

    if (response == null || response.choices() == null || response.choices().isEmpty()) {
      throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
    }

    String content = response.choices().get(0).message().content();
    return parseResponse(content);
  }

  private AiStudyPlan generateFallback(GenerateStudyPlanCommand command, Throwable t) {
    if (t instanceof BusinessException e)
      throw e;
    if (t instanceof WebClientResponseException e) {
      throw new BusinessException(ErrorCode.OPENAI_API_ERROR, "OpenAI API 오류: " + e.getStatusCode());
    }
    throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
  }

  private OpenAiRequest buildRequest(GenerateStudyPlanCommand command) {
    return new OpenAiRequest(
        openAiProperties.model(),
        List.of(
            new OpenAiRequest.Message("system", buildSystemPrompt()),
            new OpenAiRequest.Message("user", buildUserMessage(command))),
        16000);
  }

  private String buildSystemPrompt() {
    return """
        You are an expert study planner for Korean students preparing for exams.
        Create a highly detailed, day-by-day study schedule and return ONLY a JSON object.

        You MUST follow this exact JSON schema:
        {
          "plan_summary": "전체 학습 계획 요약 (3-4문장, 한국어, 전략과 주차별 흐름 포함)",
          "weekly_goals": ["1주차: 구체적 목표와 다룰 파트 명시", "2주차: ...", ...],
          "schedules": [
            {
              "title": "파트5 문법: 동사의 형태 집중 훈련",
              "memo": "동사/형용사/부사 구별 문제 30문항 풀기 → 오답 원인 분석 → 핵심 문법 규칙 노트 정리",
              "date": "YYYY-MM-DD",
              "start_time": "HH:mm",
              "end_time": "HH:mm",
              "week": 1
            }
          ]
        }

        Strict rules:
        - 모든 텍스트는 한국어로 작성
        - 반드시 시작일(startDate)부터 시험 전날(examDate - 1일)까지 총 학습 일수만큼 schedule을 생성하세요.
          어떤 날도 빠뜨리면 안 됩니다. 각 날짜에 정확히 1개의 항목을 생성하세요.
        - ⛔ 시험 당일(examDate)은 schedule에 절대 포함하지 마세요. 시험 전날이 마지막 학습일입니다.
        - start_time은 userRequest에 명시된 시작 시간을 사용하세요.
          end_time = start_time + daily_hours (정확히 daily_hours 시간 후)
          예: 시작 14:00, daily_hours=4 → end_time=18:00
        - 각 title은 반드시 [파트/영역명 + 세부 주제]를 포함해야 합니다
          나쁜 예: "파트5 공부", "토익 학습"
          좋은 예: "파트5 문법: 품사 구별과 어휘 유형 30문제", "파트3 대화 유형: 요청·제안 표현 집중"
        - 각 memo는 반드시 그 날 세션에서 할 구체적인 활동을 2~3줄로 작성하세요
          예: "RC 파트5 문제집 p.120~140 풀기 → 오답 분석 → 틀린 문법 규칙 정리"
        - 시험의 모든 파트/영역을 균형 있게 커버하세요
          (토익 → 파트1~7, 수능 → 국어/수학/영어/탐구)
        - 중점 학습 영역(focusAreas)은 다른 영역보다 약 2배 많은 세션을 배정하되,
          나머지 파트도 반드시 포함하세요
        - 학습 단계를 3단계로 구성하세요 (날짜 범위 계산 후 빈 날 없이 채울 것):
          1단계(전체 기간 60%): 파트별 개념 학습 + 기본 문제풀이
          2단계(전체 기간 30%): 약점 보강 + 실전 문제풀이
          3단계(마지막 10% 또는 최소 3일): 전 파트 실전 모의고사 + 오답 총정리
        - Return only raw JSON, no markdown code block, no extra text
        """;
  }

  private String buildUserMessage(GenerateStudyPlanCommand command) {
    String focusAreas = command.focusAreas() == null || command.focusAreas().isEmpty()
        ? "없음"
        : String.join(", ", command.focusAreas());

    long totalDays = java.time.temporal.ChronoUnit.DAYS.between(command.startDate(), command.examDate());

    String base = """
        시험명: %s
        교재: %s
        시작일: %s
        시험일: %s (이 날은 schedule에 포함하지 마세요)
        총 학습 일수: %d일 (%s ~ %s, 시험 전날까지)
        하루 학습 시간: %d시간
        기출문제 회독 수: %d회
        중점 학습 영역: %s
        """.formatted(
        command.examName(),
        command.textbook() != null ? command.textbook() : "미지정",
        command.startDate(),
        command.examDate(),
        totalDays,
        command.startDate(),
        command.examDate().minusDays(1),
        command.dailyHours(),
        command.pastExamRounds(),
        focusAreas);

    if (command.userRequest() != null && !command.userRequest().isBlank()) {
      base += "\n사용자 요청 원문 (이 내용을 최우선으로 반영하세요): " + command.userRequest();
    }

    return base;
  }

  private AiStudyPlan parseResponse(String content) {
    String cleanContent = content.strip();
    if (cleanContent.startsWith("```")) {
      cleanContent = cleanContent
          .replaceAll("(?s)^```[a-zA-Z]*\\n?", "")
          .replaceAll("```\\s*$", "")
          .strip();
    }

    log.debug("GPT study plan response: {}", cleanContent);

    GptStudyPlan gpt;
    try {
      gpt = objectMapper.readValue(cleanContent, GptStudyPlan.class);
    } catch (JsonProcessingException e) {
      throw new BusinessException(ErrorCode.OPENAI_PARSE_ERROR, "학습 계획 파싱 실패: " + e.getOriginalMessage());
    }

    List<AiScheduleItem> items = gpt.schedules().stream()
        .map(s -> new AiScheduleItem(s.title(), s.memo(), s.date(), s.startTime(), s.endTime(), s.week()))
        .collect(Collectors.toList());

    return new AiStudyPlan(gpt.planSummary(), gpt.weeklyGoals(), items);
  }
}
