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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String configuredApiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String modelName;

    @Value("${gemini.mock:true}")
    private boolean mockMode;

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
     * @param systemInstruction 系統指令（可為 null）
     * @param conversationHistory 歷史對話紀錄清單 [{role: "user"|"model", text: "..."}]
     * @param apiKey 自定義或預設的 Gemini API Key
     * @return AI 回覆文本
     */
    public String generateContent(String systemInstruction, List<Map<String, String>> conversationHistory, String apiKey) {
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

        // 3. Generation Config
        requestBody.put("generationConfig", Map.of(
            "temperature", 0.7,
            "maxOutputTokens", 4096
        ));

        try {
            String responseJson = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode textNode = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            if (!textNode.isMissingNode()) {
                return textNode.asText();
            } else {
                log.warn("Gemini API 返回結果解析異常: {}", responseJson);
                return "抱歉，無法從 AI 模型回傳中解析出有效文本內容。";
            }
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

    private String generateMockResponse(String systemInstruction, List<Map<String, String>> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return "尚無輸入內容。";
        }

        Map<String, String> lastMessage = conversationHistory.get(conversationHistory.size() - 1);
        String lastText = lastMessage.getOrDefault("text", "");

        if (systemInstruction != null) {
            // 第一輪修飾的模擬回覆
            return """
                > 💡 *【提示】尚未配置 GEMINI_API_KEY，以下為系統模擬的英文修飾效果。您可以在介面右上角設定 API Key 或寫入後端 application.yml。*

                ### 🌟 建議修飾版本 (Refined Versions)
                - **自然地道版 (Natural & Conversational)**:
                  "I'm writing to let you know about our current project status. We ran into a small issue with the database setup, so the release might be delayed by a week or two. Thanks for your patience!"
                - **專業商務版 (Professional & Formal)**:
                  "I am writing to provide an update on our project schedule. Due to unforeseen database connectivity challenges, our anticipated launch date has been postponed by approximately two weeks. We appreciate your understanding and flexibility."

                ### 💡 關鍵修飾解析 (Key Improvements)
                1. **動詞目的搭配**: 原文如使用 `for inform you`，標準英語文法應使用不定詞 `to inform you` 或更自然的 `to update you`。
                2. **因果關係精簡**: 避免中式英文常見的 `Because... so...` 同時出現，建議改用 `Due to [名詞片語]`。
                3. **語氣得體性**: 將生硬的 `Please kindly understand` 優化為商業上更具同理心的 `We appreciate your understanding`。

                ### 📚 實用升級片語與詞彙 (Vocabulary & Phrase Upgrades)
                - **hit a snag / run into a challenge**: 表示遇到突發小阻礙，比起單純的 problem 更生動。
                - **unforeseen challenges**: 不可預期的挑戰，適合商務正式彙報。

                ### 🎯 總結小叮嚀
                結構已經非常完整！只要注意連詞與語氣搭配，即可讓整篇文字大幅躍升母語者水準。
                """;
        } else {
            // 追加提問的模擬回覆
            return """
                針對您的追加問題：「%s」

                如果是熟識的同事或非正式場合，可以用更口語有親和力的方式表達：
                > *"Quick heads-up team: we hit a bump on the database connection, so launch is pushed back about a week. Thanks all!"*

                重點是：在同一個對話脈絡下，可以直接針對特定單字或情境繼續發問討論喔！
                """.formatted(lastText);
        }
    }
}
