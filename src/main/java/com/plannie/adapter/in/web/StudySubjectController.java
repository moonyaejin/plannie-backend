package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.StudySubjectRequest;
import com.plannie.adapter.in.web.dto.StudySubjectResponse;
import com.plannie.application.port.in.StudySubjectUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "StudySubject", description = "과목 관리 API")
@RestController
@RequestMapping("/api/study-subjects")
@RequiredArgsConstructor
public class StudySubjectController {

    private final StudySubjectUseCase studySubjectUseCase;

    @Operation(summary = "과목 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudySubjectResponse create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody StudySubjectRequest request) {

        return StudySubjectResponse.from(
                studySubjectUseCase.create(new StudySubjectUseCase.CreateCommand(
                        userId, request.name(), request.color()
                ))
        );
    }

    @Operation(summary = "과목 수정")
    @PutMapping("/{id}")
    public StudySubjectResponse update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody StudySubjectRequest request) {

        return StudySubjectResponse.from(
                studySubjectUseCase.update(new StudySubjectUseCase.UpdateCommand(
                        id, userId, request.name(), request.color()
                ))
        );
    }

    @Operation(summary = "과목 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {

        studySubjectUseCase.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 과목 목록 조회")
    @GetMapping
    public List<StudySubjectResponse> getAll(@AuthenticationPrincipal Long userId) {
        return studySubjectUseCase.getAll(userId)
                .stream().map(StudySubjectResponse::from).toList();
    }
}
