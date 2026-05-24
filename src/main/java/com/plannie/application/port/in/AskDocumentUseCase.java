package com.plannie.application.port.in;

public interface AskDocumentUseCase {

    record Command(Long userId, String question) {}

    record Answer(String question, String answer) {}

    Answer ask(Command command);
}
