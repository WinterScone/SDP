package org.example.sdpclient.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

@Service
@Primary
@ConditionalOnExpression("!'${anthropic.api.key:}'.isEmpty()")
public class ClaudeFaceRecognitionService implements FaceRecognitionService {

    private static final URI ANTHROPIC_MESSAGES_URI = URI.create("https://api.anthropic.com/v1/messages");

    private static final String PROMPT = """
        Image 1 is the enrolled patient photo.
        Image 2 is the live camera capture.

        Decide whether these are the same person.
        Be conservative. If uncertain, return false.

        Consider:
        - facial structure
        - eyes
        - nose
        - jawline
        - lighting differences
        - small pose differences
        - expression changes

        Respond ONLY with valid JSON:
        {
          "match": true or false,
          "confidence": "high" or "medium" or "low",
          "reason": "one short sentence"
        }
        """;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${anthropic.api.key:}")
    private String apiKey;

    @Value("${anthropic.model:claude-haiku-4-5-20251001}")
    private String model;

    @Value("${anthropic.version:2023-06-01}")
    private String anthropicVersion;

    public ClaudeFaceRecognitionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public byte[] prepareFaceData(byte[] imageBytes) {
        validateImage(imageBytes);
        return imageBytes;
    }

    @Override
    public boolean verify(byte[] enrolledFaceData, byte[] candidateImageBytes) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY is not configured");
        }

        try {
            String enrolledB64 = Base64.getEncoder().encodeToString(toJpegBytes(enrolledFaceData));
            String candidateB64 = Base64.getEncoder().encodeToString(toJpegBytes(candidateImageBytes));

            Map<String, Object> payload = Map.of(
                    "model", model,
                    "max_tokens", 120,
                    "messages", List.of(
                            Map.of(
                                    "role", "user",
                                    "content", List.of(
                                            Map.of("type", "text", "text", "Image 1 is the enrolled patient photo."),
                                            Map.of(
                                                    "type", "image",
                                                    "source", Map.of(
                                                            "type", "base64",
                                                            "media_type", "image/jpeg",
                                                            "data", enrolledB64
                                                    )
                                            ),
                                            Map.of("type", "text", "text", "Image 2 is the live camera capture."),
                                            Map.of(
                                                    "type", "image",
                                                    "source", Map.of(
                                                            "type", "base64",
                                                            "media_type", "image/jpeg",
                                                            "data", candidateB64
                                                    )
                                            ),
                                            Map.of("type", "text", "text", PROMPT)
                                    )
                            )
                    )
            );

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder(ANTHROPIC_MESSAGES_URI)
                    .timeout(Duration.ofSeconds(40))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", anthropicVersion)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Claude API error: HTTP " + response.statusCode() + " - " + response.body()
                );
            }

            String rawText = extractTextContent(response.body());
            if (rawText == null || rawText.isBlank()) {
                return false;
            }

            String cleaned = stripCodeFences(rawText);
            JsonNode parsed = objectMapper.readTree(cleaned);

            boolean match = parsed.path("match").asBoolean(false);
            String confidence = parsed.path("confidence").asText("low");

            if ("low".equalsIgnoreCase(confidence)) {
                return false;
            }

            return match;

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            return false;
        }
    }

    private void validateImage(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new IllegalArgumentException("Invalid image file");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid image file");
        }
    }

    private byte[] toJpegBytes(byte[] inputBytes) throws Exception {
        BufferedImage input = ImageIO.read(new ByteArrayInputStream(inputBytes));
        if (input == null) {
            throw new IllegalArgumentException("Invalid image file");
        }

        BufferedImage rgb = new BufferedImage(
                input.getWidth(),
                input.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = rgb.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(rgb, "jpg", out);
        return out.toByteArray();
    }

    private String extractTextContent(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode content = root.path("content");

        if (!content.isArray()) {
            return "";
        }

        for (JsonNode item : content) {
            if ("text".equals(item.path("type").asText())) {
                return item.path("text").asText("");
            }
        }

        return "";
    }

    private String stripCodeFences(String raw) {
        String text = raw.trim();

        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline >= 0) {
                text = text.substring(firstNewline + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
        }

        return text.trim();
    }
}
