package com.megacorp.humanresources.service;

import java.io.IOException;
import java.net.URI;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Service
public class ImageGenerationServiceImpl implements ImageGenerationService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

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
        description = "Generates an image from a prompt using Gemini API and uploads it to GCS. Optionally, an input image can be provided via 'optionalInputImageName' to guide the generation."
    )
    public String generateImage(String prompt, String optionalInputImageName, String outputImageRootName) throws IOException {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            throw new IllegalArgumentException("Environment variable GEMINI_API_KEY must be set.");
        }

        byte[] imageBytes;
        String imgBase64 = "";
        if (optionalInputImageName != null && !optionalInputImageName.isEmpty()) {
            imageBytes = fileStorageService.retrieveFile(optionalInputImageName);
            imgBase64 = Base64.getEncoder().encodeToString(imageBytes);
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image-preview:generateContent";
        String jsonPayload;
        if (imgBase64.isBlank()) {
            jsonPayload = String.format("{"
                + "\"contents\": ["
                + "  {"
                + "    \"parts\": ["
                + "      {\"text\": \"%s\"}"
                + "    ]"
                + "  }"
                + "]"
                + "}", prompt);
        } else {
            jsonPayload = String.format("{"
                + "\"contents\": ["
                + "  {"
                + "    \"parts\": ["
                + "      {\"text\": \"%s\"}"
                + ", {\"inline_data\": {\"mime_type\": \"image/jpeg\", \"data\": \"%s\"}}"
                + "    ]"
                + "  }"
                + "]"
                + "}", prompt, imgBase64);
        }

        // Build HTTP request
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("x-goog-api-key", geminiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        // Send the request and get the response
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error sending HTTP request: " + e.getMessage(), e);
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + response.statusCode() + " - " + response.body());
        }

        // Decode and save the image
        String base64EncodedString = response.body();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(base64EncodedString);

        // Navigate to the Base64 encoded image data
        String base64ImageData = rootNode
            .path("candidates")
            .get(0)
            .path("content")
            .path("parts")
            .get(1)
            .path("inlineData")
            .path("data")
            .asText();
        byte[] responseImageBytes = Base64.getDecoder().decode(base64ImageData);
        String outputImageFileName = outputImageRootName + ".png";

        String gcsImageName = "generated_images/" + outputImageFileName;
        fileStorageService.uploadFile(responseImageBytes, gcsImageName);

        return "Image saved to GCS as " + gcsImageName;
    }
}
