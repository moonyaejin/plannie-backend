package com.plannie.application.port.out;

import java.util.List;

public interface SearchSimilarChunksPort {

    List<String> searchTopK(Long userId, float[] queryEmbedding, int topK);
}
