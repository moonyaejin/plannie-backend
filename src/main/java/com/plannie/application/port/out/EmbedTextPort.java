package com.plannie.application.port.out;

import java.util.List;

public interface EmbedTextPort {

    float[] embed(String text);

    List<float[]> embedBatch(List<String> texts);
}
