package com.plannie.adapter.in.web;

import com.plannie.adapter.in.web.dto.ParseScheduleRequest;
import com.plannie.adapter.in.web.dto.ParseScheduleResponse;
import com.plannie.application.port.in.ParseScheduleUseCase;
import com.plannie.domain.schedule.Schedule;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ParseScheduleUseCase parseScheduleUseCase;

    @PostMapping("/parse")
    @ResponseStatus(HttpStatus.CREATED)
    public ParseScheduleResponse parseAndCreate(@Valid @RequestBody ParseScheduleRequest request) {
        Schedule schedule = parseScheduleUseCase.parseAndCreate(
                new ParseScheduleUseCase.ParseScheduleCommand(request.userId(), request.text())
        );
        return ParseScheduleResponse.from(schedule);
    }
}
