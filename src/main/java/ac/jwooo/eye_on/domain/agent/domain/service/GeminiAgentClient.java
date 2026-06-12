package ac.jwooo.eye_on.domain.agent.domain.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import ac.jwooo.eye_on.domain.agent.domain.entity.AgentDrivingState;
import ac.jwooo.eye_on.global.config.GeminiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiAgentClient {

    private static final String SYSTEM_INSTRUCTION = """
            너는 Eye-On 서비스의 졸음 방지 AI 동승자다.
            운전자를 방해하지 않게 한국어로 아주 짧고 자연스럽게 말한다.
            응답은 보통 2문장, 사용자가 농담이나 이야기를 요청하면 최대 3문장까지 가능하다.
            사용자가 재미있는 이야기나 농담을 요청하면 짧고 밝은 이야기를 먼저 해준다.
            졸음이나 수면 위험이 있으면 대화보다 환기, 휴식, 안전한 정차를 권한다.
            의학적 진단, 긴 설명, 복잡한 행동 요구는 하지 않는다.
            """;

    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public GeminiAgentReply generateReply(AgentDrivingState drivingState, String message) {
        if (!geminiProperties.hasApiKey()) {
            log.warn("Gemini API key is not configured. Returning fallback agent reply.");
            return fallbackReply(drivingState, "FALLBACK_NO_API_KEY");
        }

        try {
            String userPrompt = createUserPrompt(drivingState, message);
            log.info(
                    "Gemini agent request model={}, maxOutputTokens={}, drivingState={}, promptChars={}, prompt={}",
                    geminiProperties.normalizedModel(),
                    geminiProperties.normalizedMaxOutputTokens(),
                    drivingState,
                    userPrompt.length(),
                    printable(userPrompt)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(geminiUri())
                    .timeout(Duration.ofSeconds(geminiProperties.normalizedTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", geminiProperties.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(createRequestBody(userPrompt)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Gemini API returned non-2xx status. status={}, body={}", response.statusCode(), response.body());
                return fallbackReply(drivingState, "FALLBACK_GEMINI_HTTP_" + response.statusCode());
            }

            GeminiExtractedReply reply = extractReply(response.body());
            if (reply.text().isBlank()) {
                log.warn("Gemini agent reply was empty. finishReason={}, promptTokens={}, candidateTokens={}, totalTokens={}",
                        reply.finishReason(), reply.promptTokenCount(), reply.candidateTokenCount(), reply.totalTokenCount());
                return fallbackReply(drivingState, "FALLBACK_EMPTY_REPLY");
            }
            String sanitizedReply = sanitize(reply.text());
            log.info(
                    "Gemini agent reply source=GEMINI, finishReason={}, chars={}, promptTokens={}, candidateTokens={}, totalTokens={}, reply={}",
                    reply.finishReason(),
                    sanitizedReply.length(),
                    reply.promptTokenCount(),
                    reply.candidateTokenCount(),
                    reply.totalTokenCount(),
                    sanitizedReply
            );
            return new GeminiAgentReply(sanitizedReply, "GEMINI");
        } catch (IOException e) {
            log.warn("Failed to call Gemini API.", e);
            return fallbackReply(drivingState, "FALLBACK_IO_ERROR");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Gemini API call was interrupted.", e);
            return fallbackReply(drivingState, "FALLBACK_INTERRUPTED");
        } catch (RuntimeException e) {
            log.warn("Unexpected Gemini API error.", e);
            return fallbackReply(drivingState, "FALLBACK_RUNTIME_ERROR");
        }
    }

    private URI geminiUri() {
        return URI.create("%s/%s:generateContent".formatted(
                geminiProperties.normalizedEndpoint(),
                geminiProperties.normalizedModel()
        ));
    }

    private String createRequestBody(String userPrompt) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode systemInstruction = root.putObject("systemInstruction");
        systemInstruction.putArray("parts")
                .addObject()
                .put("text", SYSTEM_INSTRUCTION);

        ArrayNode contents = root.putArray("contents");
        ObjectNode userContent = contents.addObject();
        userContent.put("role", "user");
        userContent.putArray("parts")
                .addObject()
                .put("text", userPrompt);

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("temperature", 0.7);
        generationConfig.put("topP", 0.9);
        generationConfig.put("maxOutputTokens", geminiProperties.normalizedMaxOutputTokens());

        return objectMapper.writeValueAsString(root);
    }

    private String createUserPrompt(AgentDrivingState drivingState, String message) {
        return """
                현재 운전자 상태: %s
                사용자 말: %s

                지금 상황에 맞게 동승자처럼 짧게 답해줘.
                사용자가 재미있는 얘기나 농담을 요청했다면, 안전을 해치지 않는 짧은 농담이나 이야기를 해줘.
                """.formatted(drivingState, message.trim());
    }

    private GeminiExtractedReply extractReply(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidate = root.path("candidates").path(0);
        JsonNode parts = candidate
                .path("content")
                .path("parts");
        String finishReason = candidate.path("finishReason").asText("UNKNOWN");
        JsonNode usageMetadata = root.path("usageMetadata");

        if (!parts.isArray()) {
            return new GeminiExtractedReply(
                    "",
                    finishReason,
                    usageMetadata.path("promptTokenCount").asInt(-1),
                    usageMetadata.path("candidatesTokenCount").asInt(-1),
                    usageMetadata.path("totalTokenCount").asInt(-1)
            );
        }

        StringBuilder reply = new StringBuilder();
        for (JsonNode part : parts) {
            String text = part.path("text").asText("");
            if (!text.isBlank()) {
                if (!reply.isEmpty()) {
                    reply.append(' ');
                }
                reply.append(text);
            }
        }
        return new GeminiExtractedReply(
                reply.toString(),
                finishReason,
                usageMetadata.path("promptTokenCount").asInt(-1),
                usageMetadata.path("candidatesTokenCount").asInt(-1),
                usageMetadata.path("totalTokenCount").asInt(-1)
        );
    }

    private String sanitize(String reply) {
        String compactReply = reply.replaceAll("\\s+", " ").trim();
        if (compactReply.length() <= 180) {
            return compactReply;
        }
        return compactReply.substring(0, 180).trim() + "...";
    }

    private String printable(String text) {
        String compactText = text.replaceAll("\\s+", " ").trim();
        if (compactText.length() <= 700) {
            return compactText;
        }
        return compactText.substring(0, 700).trim() + "...";
    }

    private GeminiAgentReply fallbackReply(AgentDrivingState drivingState, String source) {
        String reply = switch (drivingState) {
            case SLEEP -> "지금은 대화보다 안전이 먼저예요. 알람을 듣고 가능한 곳에 정차해 쉬어가요.";
            case DROWSY -> "눈이 조금 무거워 보여요. 창문을 조금 열고 가까운 곳에서 잠깐 쉬어가요.";
            case AWAKE, NORMAL -> "좋아요. 제가 옆에서 같이 집중할게요. 필요하면 편하게 말 걸어주세요.";
        };
        return new GeminiAgentReply(reply, source);
    }

    private record GeminiExtractedReply(
            String text,
            String finishReason,
            int promptTokenCount,
            int candidateTokenCount,
            int totalTokenCount
    ) {
    }
}
