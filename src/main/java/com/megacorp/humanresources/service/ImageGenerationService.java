package com.megacorp.humanresources.service;

import java.io.IOException;

public interface ImageGenerationService {
    String generateImage(String prompt, String optionalInputImageName, String outputImageRootName) throws IOException;
}
