package ac.jwooo.eye_on.domain.agent.domain.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import ac.jwooo.eye_on.domain.agent.domain.entity.AgentDrivingState;
import ac.jwooo.eye_on.global.config.GeminiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiAgentClient {

    private static final int MAX_GEMINI_ATTEMPTS = 2;
    private static final Duration RETRY_DELAY = Duration.ofMillis(400);
    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    private static final URI GCE_PROJECT_ID_URI = URI.create("http://metadata.google.internal/computeMetadata/v1/project/project-id");
    private static final String SYSTEM_INSTRUCTION = """
            너는 Eye-On 서비스의 졸음 방지 AI 동승자다.
            너의 역할은 옆좌석의 친근한 동승자처럼 자연스럽게 대화하며 운전자의 졸음을 줄이는 것이다.
            한국어로 말하고, 딱딱한 상담사나 경고문처럼 말하지 않는다.
            사용자가 농담, 잡담, 이야기, 질문을 하면 가능한 한 먼저 짧고 재밌게 받아준다.
            일반적인 대화 요청에는 회피하지 말고 자연스럽게 답한다.
            단, 운전자가 졸리다고 말하거나 상태가 DROWSY/SLEEP이면 답변 끝에 안전하게 환기, 휴식, 정차를 부드럽게 권한다.
            응답은 보통 2~4문장으로 한다. 너무 길게 설교하지 않는다.
            불법, 위험 행동을 유도하는 요청은 따르지 말고 안전한 대안을 말한다.
            """;

    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private volatile GoogleCredentials vertexCredentials;
    private volatile String discoveredProjectId;

    public GeminiAgentReply generateReply(AgentDrivingState drivingState, String message) {
        if (!geminiProperties.isVertexProvider() && !geminiProperties.hasApiKey()) {
            log.warn("Gemini API key is not configured. Returning fallback agent reply.");
            return fallbackReply(drivingState, "FALLBACK_NO_API_KEY");
        }

        try {
            String userPrompt = createUserPrompt(drivingState, message);
            log.info(
                    "Gemini agent request provider={}, model={}, maxOutputTokens={}, thinkingBudget={}, drivingState={}, promptChars={}, prompt={}",
                    geminiProperties.normalizedProvider(),
                    geminiProperties.normalizedModel(),
                    geminiProperties.normalizedMaxOutputTokens(),
                    geminiProperties.normalizedThinkingBudget(),
                    drivingState,
                    userPrompt.length(),
                    printable(userPrompt)
            );

            HttpResponse<String> response = sendGeminiRequest(createRequestBody(userPrompt));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Gemini API returned non-2xx status. status={}, body={}", response.statusCode(), response.body());
                return fallbackReply(drivingState, "FALLBACK_GEMINI_HTTP_" + response.statusCode());
            }

            GeminiExtractedReply reply = extractReply(response.body());
            if (reply.text().isBlank()) {
                log.warn("Gemini agent reply was empty. finishReason={}, promptTokens={}, candidateTokens={}, thoughtsTokens={}, totalTokens={}",
                        reply.finishReason(), reply.promptTokenCount(), reply.candidateTokenCount(), reply.thoughtsTokenCount(), reply.totalTokenCount());
                return fallbackReply(drivingState, "FALLBACK_EMPTY_REPLY");
            }
            String sanitizedReply = sanitize(reply.text());
            log.info(
                    "Gemini agent reply source={}, finishReason={}, chars={}, promptTokens={}, candidateTokens={}, thoughtsTokens={}, totalTokens={}, reply={}",
                    geminiProperties.replySource(),
                    reply.finishReason(),
                    sanitizedReply.length(),
                    reply.promptTokenCount(),
                    reply.candidateTokenCount(),
                    reply.thoughtsTokenCount(),
                    reply.totalTokenCount(),
                    sanitizedReply
            );
            return new GeminiAgentReply(sanitizedReply, geminiProperties.replySource());
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

    private HttpResponse<String> sendGeminiRequest(String requestBody) throws IOException, InterruptedException {
        for (int attempt = 1; attempt <= MAX_GEMINI_ATTEMPTS; attempt++) {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(geminiUri())
                    .timeout(Duration.ofSeconds(geminiProperties.normalizedTimeoutSeconds()))
                    .header("Content-Type", "application/json");
            addAuthHeader(requestBuilder);

            HttpRequest request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (!isRetryableStatus(response.statusCode()) || attempt == MAX_GEMINI_ATTEMPTS) {
                return response;
            }

            log.warn(
                    "Gemini API returned retryable status. attempt={}, status={}, delayMillis={}, body={}",
                    attempt,
                    response.statusCode(),
                    RETRY_DELAY.toMillis(),
                    response.body()
            );
            Thread.sleep(RETRY_DELAY.toMillis());
        }
        throw new IOException("Gemini request retry loop exited unexpectedly.");
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode == 503;
    }

    private URI geminiUri() throws IOException, InterruptedException {
        if (geminiProperties.isVertexProvider()) {
            return vertexGeminiUri();
        }
        return URI.create("%s/%s:generateContent".formatted(
                geminiProperties.normalizedEndpoint(),
                geminiProperties.normalizedModel()
        ));
    }

    private URI vertexGeminiUri() throws IOException, InterruptedException {
        String projectId = resolveVertexProjectId();
        if (projectId.isBlank()) {
            throw new IOException("GCP project ID is not configured and could not be discovered from metadata server.");
        }
        return URI.create("%s/projects/%s/locations/%s/publishers/google/models/%s:generateContent".formatted(
                geminiProperties.normalizedVertexEndpoint(),
                projectId,
                geminiProperties.normalizedLocation(),
                geminiProperties.normalizedModel()
        ));
    }

    private void addAuthHeader(HttpRequest.Builder requestBuilder) throws IOException {
        if (geminiProperties.isVertexProvider()) {
            requestBuilder.header("Authorization", "Bearer " + vertexAccessToken());
            return;
        }
        requestBuilder.header("x-goog-api-key", geminiProperties.apiKey());
    }

    private String vertexAccessToken() throws IOException {
        GoogleCredentials credentials = vertexCredentials();
        credentials.refreshIfExpired();
        AccessToken accessToken = credentials.getAccessToken();
        if (accessToken == null || accessToken.getTokenValue() == null || accessToken.getTokenValue().isBlank()) {
            credentials.refresh();
            accessToken = credentials.getAccessToken();
        }
        if (accessToken == null || accessToken.getTokenValue() == null || accessToken.getTokenValue().isBlank()) {
            throw new IOException("Could not load a GCP access token for Vertex AI.");
        }
        return accessToken.getTokenValue();
    }

    private GoogleCredentials vertexCredentials() throws IOException {
        GoogleCredentials credentials = vertexCredentials;
        if (credentials != null) {
            return credentials;
        }
        synchronized (this) {
            if (vertexCredentials == null) {
                credentials = GoogleCredentials.getApplicationDefault();
                if (credentials.createScopedRequired()) {
                    credentials = credentials.createScoped(List.of(CLOUD_PLATFORM_SCOPE));
                }
                vertexCredentials = credentials;
            }
            return vertexCredentials;
        }
    }

    private String resolveVertexProjectId() throws IOException, InterruptedException {
        if (geminiProperties.hasConfiguredProjectId()) {
            return geminiProperties.normalizedProjectId();
        }
        String projectId = discoveredProjectId;
        if (projectId != null) {
            return projectId;
        }
        synchronized (this) {
            if (discoveredProjectId == null) {
                discoveredProjectId = discoverProjectIdFromMetadata().orElse("");
            }
            return discoveredProjectId;
        }
    }

    private Optional<String> discoverProjectIdFromMetadata() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(GCE_PROJECT_ID_URI)
                .timeout(Duration.ofSeconds(2))
                .header("Metadata-Flavor", "Google")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Could not discover GCP project ID from metadata server. status={}", response.statusCode());
            return Optional.empty();
        }
        String projectId = response.body().trim();
        if (projectId.isBlank()) {
            log.warn("GCP metadata server returned an empty project ID.");
            return Optional.empty();
        }
        log.info("Discovered GCP project ID from metadata server for Vertex AI Gemini.");
        return Optional.of(projectId);
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
        generationConfig.putObject("thinkingConfig")
                .put("thinkingBudget", geminiProperties.normalizedThinkingBudget());

        return objectMapper.writeValueAsString(root);
    }

    private String createUserPrompt(AgentDrivingState drivingState, String message) {
        return """
                현재 운전자 상태: %s
                사용자 말: %s

                지금 상황에 맞게 옆자리 동승자처럼 자연스럽게 답해줘.
                사용자가 재미있는 얘기나 농담을 요청했다면, 먼저 짧고 밝은 농담이나 이야기를 해줘.
                사용자가 졸리다고 말했거나 상태가 DROWSY/SLEEP이면 마지막에 쉬어가자는 말을 부드럽게 붙여줘.
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
                    usageMetadata.path("thoughtsTokenCount").asInt(-1),
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
                usageMetadata.path("thoughtsTokenCount").asInt(-1),
                usageMetadata.path("totalTokenCount").asInt(-1)
        );
    }

    private String sanitize(String reply) {
        String compactReply = reply.replaceAll("\\s+", " ").trim();
        if (compactReply.length() <= 420) {
            return compactReply;
        }
        return compactReply.substring(0, 420).trim() + "...";
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
            int thoughtsTokenCount,
            int totalTokenCount
    ) {
    }
}
