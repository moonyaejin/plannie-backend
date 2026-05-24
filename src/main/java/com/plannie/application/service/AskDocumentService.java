package com.plannie.application.service;

import com.plannie.application.port.in.AskDocumentUseCase;
import com.plannie.application.port.out.EmbedTextPort;
import com.plannie.application.port.out.GenerateRagAnswerPort;
import com.plannie.application.port.out.SearchSimilarChunksPort;
import com.plannie.common.exception.BusinessException;
import com.plannie.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AskDocumentService implements AskDocumentUseCase {

    private static final int TOP_K = 5;

    private final EmbedTextPort embedTextPort;
    private final SearchSimilarChunksPort searchSimilarChunksPort;
    private final GenerateRagAnswerPort generateRagAnswerPort;

    @Override
    public Answer ask(Command command) {
        float[] queryEmbedding = embedTextPort.embed(command.question());
        List<String> contextChunks = searchSimilarChunksPort.searchTopK(command.userId(), queryEmbedding, TOP_K);

        if (contextChunks.isEmpty()) {
            throw new BusinessException(ErrorCode.DOCUMENT_NO_CONTEXT);
        }

        String answer = generateRagAnswerPort.generate(command.question(), contextChunks);
        return new Answer(command.question(), answer);
    }
}
