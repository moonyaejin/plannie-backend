package com.plannie.application.port.in;

public interface AskDocumentUseCase {

    record Command(Long userId, String question) {}

    record Answer(String answer, String question) {}

    Answer ask(Command command);
}
