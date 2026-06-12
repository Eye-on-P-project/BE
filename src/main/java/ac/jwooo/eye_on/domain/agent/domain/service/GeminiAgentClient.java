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
            응답은 최대 2문장이다.
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
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(geminiUri())
                    .timeout(Duration.ofSeconds(geminiProperties.normalizedTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", geminiProperties.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(createRequestBody(drivingState, message)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Gemini API returned non-2xx status. status={}, body={}", response.statusCode(), response.body());
                return fallbackReply(drivingState, "FALLBACK_GEMINI_HTTP_" + response.statusCode());
            }

            String reply = extractReply(response.body());
            if (reply.isBlank()) {
                return fallbackReply(drivingState, "FALLBACK_EMPTY_REPLY");
            }
            return new GeminiAgentReply(sanitize(reply), "GEMINI");
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

    private String createRequestBody(AgentDrivingState drivingState, String message) throws IOException {
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
                .put("text", createUserPrompt(drivingState, message));

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
                """.formatted(drivingState, message.trim());
    }

    private String extractReply(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode parts = root.path("candidates")
                .path(0)
                .path("content")
                .path("parts");

        if (!parts.isArray()) {
            return "";
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
        return reply.toString();
    }

    private String sanitize(String reply) {
        String compactReply = reply.replaceAll("\\s+", " ").trim();
        if (compactReply.length() <= 180) {
            return compactReply;
        }
        return compactReply.substring(0, 180).trim() + "...";
    }

    private GeminiAgentReply fallbackReply(AgentDrivingState drivingState, String source) {
        String reply = switch (drivingState) {
            case SLEEP -> "지금은 대화보다 안전이 먼저예요. 알람을 듣고 가능한 곳에 정차해 쉬어가요.";
            case DROWSY -> "눈이 조금 무거워 보여요. 창문을 조금 열고 가까운 곳에서 잠깐 쉬어가요.";
            case AWAKE, NORMAL -> "좋아요. 제가 옆에서 같이 집중할게요. 필요하면 편하게 말 걸어주세요.";
        };
        return new GeminiAgentReply(reply, source);
    }
}
