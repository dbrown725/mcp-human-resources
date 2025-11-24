package com.megacorp.humanresources.service;

import java.io.IOException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Content;
import java.util.ArrayList;

@Service
public class ImageGenerationServiceImpl implements ImageGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(ImageGenerationServiceImpl.class);

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${google.gemini.image.generation.model}")
    private String geminiImageGenerationModel;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Generates an image from a prompt using the Gemini API and uploads it to Google Cloud Storage.
     *
     * <p>This method sends a request to the Gemini generative image API with the provided prompt.
     * Optionally, an input image can be supplied via {@code optionalInputImageName} to guide the generation.
     * The response is parsed for the Base64-encoded image data, which is decoded and uploaded to Google Cloud Storage
     * using {@code fileStorageService}. The image is saved under "generated_images/" with the specified root name and a ".png" extension.
     *
     * @param prompt the textual prompt for image generation
     * @param optionalInputImageName the name of an optional input image file to guide generation (may be null or empty)
     * @param outputImageRootName the root name for the output image file (without extension)
     * @return a message indicating the image was saved to Google Cloud Storage, including the storage path
     * @throws IOException if there is an error processing the HTTP response or reading image data
     * @throws IllegalArgumentException if the Gemini API key is not set
     * @throws RuntimeException if the HTTP request fails or the response is invalid
     */
    @Override
    @Tool(
        name = "generateImage",
        description = "Generates an image from a prompt using Gemini API and uploads it to GCS. Optionally, input images can be provided via 'optionalInputImageNames' array to guide the generation."
    )
    public String generateImage(String prompt, String[] optionalInputImageNames, String outputImageRootName) throws IOException {
        logger.info("generateImage called with prompt: '{}', optionalInputImageNames: '{}', outputImageRootName: '{}'",
            prompt,
            optionalInputImageNames != null ? String.join(", ", optionalInputImageNames) : "null",
            outputImageRootName
        );
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            logger.error("Gemini API key is not set or is empty.");
            throw new IllegalArgumentException("Environment variable GEMINI_API_KEY must be set.");
        }

        try (Client client = new Client()) {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseModalities("TEXT", "IMAGE")
                    .build();

            ArrayList<Part> inputParts = new ArrayList<>();
            if (optionalInputImageNames != null) {
                for (String imageName : optionalInputImageNames) {
                    if (imageName != null && !imageName.isEmpty()) {
                        inputParts.add(Part.fromBytes(
                            fileStorageService.retrieveFile(imageName),
                            "image/jpeg"));
                    }
                }
            }

            Part textPart = Part.fromText(prompt);

            Part[] combinedParts = new Part[inputParts.size() + 1];
            combinedParts[0] = textPart;
            for (int i = 0; i < inputParts.size(); i++) {
                combinedParts[i + 1] = inputParts.get(i);
            }

            GenerateContentResponse response = client.models.generateContent(
                geminiImageGenerationModel,
                Content.fromParts(combinedParts),
                config);

            for (Part part : response.parts()) {
                if (part.text().isPresent()) {
                    logger.info(part.text().get());
                } else if (part.inlineData().isPresent()) {
                    var blob = part.inlineData().get();
                    if (blob.data().isPresent()) {
                        byte[] responseImageBytes = blob.data().get();
                        String outputImageFileName = outputImageRootName + ".png";

                        String gcsImageName = "generated_images/" + outputImageFileName;
                        fileStorageService.uploadFile(responseImageBytes, gcsImageName);
                        return "Image saved to GCS as " + gcsImageName;
                    }
                }
            }

        }

        return "Image generation failed.";
    }
}
