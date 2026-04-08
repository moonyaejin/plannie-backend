package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.StudyPlanRequest;
import com.plannie.adapter.in.web.dto.StudyPlanResponse;
import com.plannie.application.port.in.GenerateStudyPlanUseCase;
import com.plannie.application.port.in.GenerateStudyPlanUseCase.GenerateStudyPlanCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "StudyPlan", description = "AI 학습 계획 생성 API")
@RestController
@RequestMapping("/api/study-plans")
@RequiredArgsConstructor
public class StudyPlanController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final GenerateStudyPlanUseCase generateStudyPlanUseCase;

    @Operation(summary = "학습 계획 생성",
               description = "시험/교재 정보를 입력하면 AI가 학습 일정을 자동으로 생성합니다")
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public StudyPlanResponse generate(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody StudyPlanRequest request) {

        GenerateStudyPlanCommand command = new GenerateStudyPlanCommand(
                userId,
                request.examName(),
                request.startDate(),
                request.examDate(),
                request.textbook(),
                request.dailyHours(),
                request.pastExamRounds(),
                request.focusAreas()
        );

        return StudyPlanResponse.from(generateStudyPlanUseCase.generate(command));
    }
}
