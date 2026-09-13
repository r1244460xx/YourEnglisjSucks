package com.yourenglishsucks.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String configuredApiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String modelName;

    @Value("${gemini.mock:true}")
    private boolean mockMode;

    @Value("${gemini.temperature:0.55}")
    private double temperature;

    @Value("${gemini.top-p:0.95}")
    private double topP;

    @Value("${gemini.max-output-tokens:1000}")
    private int maxOutputTokens;

    @Value("${gemini.presence-penalty:0.2}")
    private double presencePenalty;

    @Value("${gemini.thinking-budget:0}")
    private int thinkingBudget;

    public GeminiService(ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().build();
        this.objectMapper = objectMapper;
    }

    public boolean hasConfiguredApiKey() {
        return configuredApiKey != null && !configuredApiKey.isBlank();
    }

    public String resolveApiKey(String requestApiKey) {
        if (requestApiKey != null && !requestApiKey.isBlank()) {
            return requestApiKey.trim();
        }
        if (configuredApiKey != null && !configuredApiKey.isBlank()) {
            return configuredApiKey.trim();
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        return null;
    }

    /**
     * 發送請求至 Google Gemini API
     */
    public String generateContent(String systemInstruction, List<Map<String, String>> conversationHistory, String apiKey) {
        return generateContent(systemInstruction, conversationHistory, apiKey, null);
    }

    /**
     * 發送請求至 Google Gemini API (支援自定義 responseSchema)
     */
    public String generateContent(String systemInstruction, List<Map<String, String>> conversationHistory, String apiKey, Map<String, Object> responseSchema) {
        String effectiveKey = resolveApiKey(apiKey);

        // 若啟用 mock 模式或未配置 key，直接回傳符合四大模組格式的模擬輸出
        if (mockMode || effectiveKey == null || effectiveKey.isBlank()) {
            log.info("【Mock AI Mode 啟用】返回結構化模擬結果，便於精準驗證輸入輸出 Use Case。");
            return generateMockResponse(systemInstruction, conversationHistory);
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + effectiveKey;

        Map<String, Object> requestBody = new HashMap<>();

        // 1. 若有系統指令 (例如第一輪修飾專屬 Skill)
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            requestBody.put("system_instruction", Map.of(
                "parts", List.of(Map.of("text", systemInstruction))
            ));
        }

        // 2. 組裝 contents
        List<Map<String, Object>> contents = new ArrayList<>();
        for (Map<String, String> item : conversationHistory) {
            String role = "user".equalsIgnoreCase(item.get("role")) ? "user" : "model";
            String text = item.getOrDefault("text", "");
            contents.add(Map.of(
                "role", role,
                "parts", List.of(Map.of("text", text))
            ));
        }
        requestBody.put("contents", contents);

        // 3. Generation Config (嚴格限制 JSON 回覆、1000 Tokens、temperature 0.55、topP 0.95、presencePenalty 0.2、thinkingBudget 0、responseSchema)
        Map<String, Object> genConfig = new HashMap<>();
        genConfig.put("temperature", temperature);
        genConfig.put("topP", topP);
        genConfig.put("maxOutputTokens", maxOutputTokens);
        genConfig.put("presencePenalty", presencePenalty);
        genConfig.put("responseMimeType", "application/json");
        genConfig.put("thinkingConfig", Map.of("thinkingBudget", thinkingBudget));
        if (responseSchema != null && !responseSchema.isEmpty()) {
            genConfig.put("responseSchema", responseSchema);
        }
        requestBody.put("generationConfig", genConfig);

        try {
            String responseJson = restClient.post()
                    .uri(url)
                    .header("x-goog-api-key", effectiveKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode partsNode = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts");

            if (partsNode.isArray() && !partsNode.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode part : partsNode) {
                    JsonNode textNode = part.path("text");
                    if (!textNode.isMissingNode()) {
                        sb.append(textNode.asText());
                    }
                }
                if (!sb.isEmpty()) {
                    return sb.toString();
                }
            }
            log.warn("Gemini API 返回結果解析異常: {}", responseJson);
            return "抱歉，無法從 AI 模型回傳中解析出有效文本內容。";
        } catch (RestClientResponseException e) {
            log.error("呼叫 Gemini API 失敗: HTTP {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "【Gemini API 呼叫失敗 (" + e.getStatusCode() + ")】\n" +
                   "錯誤詳情：" + e.getResponseBodyAsString() + "\n\n" +
                   "請檢查您的 Gemini API Key 是否正確且具備存取權限。";
        } catch (Exception e) {
            log.error("呼叫 Gemini API 發生未預期錯誤", e);
            return "連線至 Gemini API 時發生異常：" + e.getMessage();
        }
    }

    /**
     * 端到端真流式輸出 (SSE Stream)
     */
    public void streamContent(
            String systemInstruction,
            List<Map<String, String>> conversationHistory,
            String apiKey,
            Consumer<String> onChunkReceived,
            Runnable onComplete,
            Consumer<Throwable> onError) {
        streamContent(systemInstruction, conversationHistory, apiKey, null, onChunkReceived, onComplete, onError);
    }

    /**
     * 端到端真流式輸出 (SSE Stream, 支援自定義 responseSchema)
     */
    public void streamContent(
            String systemInstruction,
            List<Map<String, String>> conversationHistory,
            String apiKey,
            Map<String, Object> responseSchema,
            Consumer<String> onChunkReceived,
            Runnable onComplete,
            Consumer<Throwable> onError) {

        String effectiveKey = resolveApiKey(apiKey);

        // 若啟用 mock 模式或未配置 key，非同步推播模擬打字機流
        if (mockMode || effectiveKey == null || effectiveKey.isBlank()) {
            log.info("【Mock AI Streaming Mode 啟用】非同步模擬輸出打字機效果。");
            CompletableFuture.runAsync(() -> {
                try {
                    String mockReply = generateMockResponse(systemInstruction, conversationHistory);
                    int chunkSize = 6;
                    for (int i = 0; i < mockReply.length(); i += chunkSize) {
                        int end = Math.min(i + chunkSize, mockReply.length());
                        onChunkReceived.accept(mockReply.substring(i, end));
                        Thread.sleep(30);
                    }
                    onComplete.run();
                } catch (Exception e) {
                    onError.accept(e);
                }
            });
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String streamUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":streamGenerateContent?alt=sse&key=" + effectiveKey;

                Map<String, Object> requestBody = new HashMap<>();
                if (systemInstruction != null && !systemInstruction.isBlank()) {
                    requestBody.put("system_instruction", Map.of(
                            "parts", List.of(Map.of("text", systemInstruction))
                    ));
                }

                List<Map<String, Object>> contents = new ArrayList<>();
                for (Map<String, String> item : conversationHistory) {
                    String role = "user".equalsIgnoreCase(item.get("role")) ? "user" : "model";
                    String text = item.getOrDefault("text", "");
                    contents.add(Map.of(
                            "role", role,
                            "parts", List.of(Map.of("text", text))
                    ));
                }
                requestBody.put("contents", contents);
                Map<String, Object> genConfig = new HashMap<>();
                genConfig.put("temperature", temperature);
                genConfig.put("topP", topP);
                genConfig.put("maxOutputTokens", maxOutputTokens);
                genConfig.put("presencePenalty", presencePenalty);
                genConfig.put("responseMimeType", "application/json");
                genConfig.put("thinkingConfig", Map.of("thinkingBudget", thinkingBudget));
                if (responseSchema != null && !responseSchema.isEmpty()) {
                    genConfig.put("responseSchema", responseSchema);
                }
                requestBody.put("generationConfig", genConfig);

                String jsonBody = objectMapper.writeValueAsString(requestBody);

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(15))
                        .build();

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(streamUrl))
                        .timeout(Duration.ofSeconds(120))
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", effectiveKey)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<Stream<String>> resp = client.send(req, HttpResponse.BodyHandlers.ofLines());

                if (resp.statusCode() == 429) {
                    if (!"gemini-3.5-flash-lite".equalsIgnoreCase(modelName)) {
                        log.warn("模型 {} 觸發 429 額度上限，自動切換至高額度備援模型 gemini-3.5-flash-lite 重試...", modelName);
                        String fallbackUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:streamGenerateContent?alt=sse&key=" + effectiveKey;
                        HttpRequest fallbackReq = HttpRequest.newBuilder()
                                .uri(URI.create(fallbackUrl))
                                .timeout(Duration.ofSeconds(120))
                                .header("Content-Type", "application/json")
                                .header("x-goog-api-key", effectiveKey)
                                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                                .build();
                        resp = client.send(fallbackReq, HttpResponse.BodyHandlers.ofLines());
                    }
                }

                if (resp.statusCode() >= 400) {
                    StringBuilder errSb = new StringBuilder();
                    resp.body().forEach(errSb::append);
                    String rawErr = errSb.toString();
                    if (resp.statusCode() == 429) {
                        throw new RuntimeException("【API 呼叫頻率已達 Google 免費額度上限 (429 Too Many Requests)】\n請稍候約 30~60 秒後點擊「點擊重試 🔄」，或至 Google AI Studio 綁定計費帳戶以提升額度。");
                    }
                    throw new RuntimeException("Gemini API Error (" + resp.statusCode() + "): " + rawErr);
                }

                resp.body().forEach(line -> {
                    if (line != null && line.startsWith("data: ")) {
                        String payload = line.substring(6).trim();
                        if (!payload.isBlank() && !"[DONE]".equals(payload)) {
                            try {
                                JsonNode root = objectMapper.readTree(payload);
                                JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
                                if (parts.isArray()) {
                                    for (JsonNode part : parts) {
                                        JsonNode text = part.path("text");
                                        if (!text.isMissingNode() && !text.asText().isEmpty()) {
                                            onChunkReceived.accept(text.asText());
                                        }
                                    }
                                }
                            } catch (Exception parseEx) {
                                log.warn("解析 Gemini Stream chunk 異常: {}", payload, parseEx);
                            }
                        }
                    }
                });

                onComplete.run();
            } catch (Exception ex) {
                log.error("Gemini 串流過程發生異常", ex);
                onError.accept(ex);
            }
        });
    }

    private String generateMockResponse(String systemInstruction, List<Map<String, String>> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return "{\"reply\":\"尚無輸入內容。\"}";
        }

        Map<String, String> lastMessage = conversationHistory.get(conversationHistory.size() - 1);
        String lastText = lastMessage.getOrDefault("text", "");

        if (systemInstruction != null) {
            // 第一輪修飾的模擬回覆 (遵循嚴格 JSON 結構規格)
            return """
                {
                  "refinedText": {
                    "casual": "I'm writing to let you know about our current project status. We ran into a small issue with the database setup, so the release might be delayed by a week or two. Thanks for your patience!",
                    "formal": "I am writing to provide an update on our project schedule. Due to unforeseen database connectivity challenges, our anticipated launch date has been postponed by approximately two weeks. We appreciate your understanding and flexibility."
                  },
                  "grammarAnalysis": [
                    {
                      "original": "for update",
                      "explanation": "不定詞應使用 to update 或 to give you an update，for 後面通常接名詞或動名詞。"
                    },
                    {
                      "original": "because we have error",
                      "explanation": "搭配動詞應使用 encountered an error 或 ran into an issue 更為道地。"
                    }
                  ],
                  "rationale": [
                    "語氣得體性：商務語境中使用 unforeseen challenges 比單純直白抱怨 we have error 顯得更加專業且負責。",
                    "口語生活感：口語溝通中以 ran into a small issue 表達碰上小問題，親切自然。"
                  ],
                  "idiomsAndUpgrades": [
                    {
                      "phrase": "hit a snag / run into a bump",
                      "meaning": "遇到突發小阻礙。",
                      "example": "We hit a small snag during deployment, but we've already fixed it. (我們部署時遇到一點小阻礙，但已經修復了。)"
                    }
                  ]
                }
                """;
        } else {
            // 追加提問的模擬回覆 (遵循嚴格 JSON 結構規格)
            String safeText = lastText.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
            return """
                {
                  "reply": "針對您的追加問題：「%s」\\n\\n如果是熟識的同事或非正式場合，可以用更口語有親和力的方式表達：\\n> *\\"Quick heads-up team: we hit a bump on the database connection, so launch is pushed back about a week. Thanks all!\\"*\\n\\n重點是：在同一個對話脈絡下，可以直接針對特定單字或情境繼續發問討論喔！"
                }
                """.formatted(safeText);
        }
    }
}
