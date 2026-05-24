package com.plannie.application.port.out;

import java.util.List;

public interface GenerateRagAnswerPort {

    String generate(String question, List<String> contextChunks);
}
