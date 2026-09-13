package com.yourenglishsucks.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SkillService {

    private static final Logger log = LoggerFactory.getLogger(SkillService.class);
    private final ObjectMapper objectMapper;

    public SkillService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 英文修飾專用 Skill 的 System Prompt。
     * 嚴格規範以 JSON 格式輸出，精準節省 Token 並確保格式穩定，杜絕任何問候語與排版漂移。
     */
    public String getEnglishPolishSystemPrompt() {
        return """
            你是一位高效、精準、直奔重點的資深英語母語編輯與寫作教練。
            
            【絕對約束 / Constraints】：
            1. 必須嚴格以合法的 JSON 格式輸出，不得夾帶任何 Markdown 程式碼標籤（例如不得使用 ```json 開頭或結尾），直接輸出純 JSON。
            2. 嚴禁任何開場白、自我介紹、問候語（如「你好」、「很高興能為你服務」等）。
            3. 嚴禁任何結尾客套話（如「希望對你有幫助」、「加油」等）。
            
            【JSON 輸出結構規格 (Schema)】：
            {
              "refinedText": {
                "casual": "自然道地口語版完整英文句子或段落",
                "formal": "專業商務正式版完整英文句子或段落"
              },
              "grammarAnalysis": [
                {
                  "original": "引述原文有問題的字句",
                  "explanation": "說明文法錯誤、時態問題、中式英文或不道地之處"
                }
              ],
              "rationale": [
                "說明關鍵用詞、介系詞、句構重組的背後英語思維與語境差異"
              ],
              "idiomsAndUpgrades": [
                {
                  "phrase": "片語或詞彙",
                  "meaning": "中文意義與用法說明",
                  "example": "英文例句 (中文翻譯)"
                }
              ]
            }
            """;
    }

    /**
     * 組裝第一次輸入時的使用者 Prompt。
     */
    public String buildFirstRoundPolishPrompt(String rawEnglishText) {
        return """
            請直接修飾以下英文文本，並依規格輸出純 JSON 物件：

            %s
            """.formatted(rawEnglishText);
    }

    /**
     * 後端配套措施：將 Gemini 回傳的嚴格 JSON 結構轉換為標準 Markdown 排版。
     * 確保前端依然擁有：
     * 1. 頂部獨立 > blockquote 引用區塊（極佳滑鼠拖曳與一鍵複製體驗）。
     * 2. 乾淨清爽的四階段條列分析。
     * 3. 具備容錯機制，若非標準 JSON 則平滑回退為原文字串。
     */
    public String formatPolishJsonToMarkdown(String jsonOrText) {
        if (jsonOrText == null || jsonOrText.isBlank()) {
            return "";
        }

        String trimmed = jsonOrText.trim();
        // 去除可能的 markdown code block 標籤包裹
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();

        if (!trimmed.startsWith("{")) {
            return jsonOrText;
        }

        try {
            JsonNode root = objectMapper.readTree(trimmed);
            StringBuilder sb = new StringBuilder();

            // 1. 🌟 修飾後英文全文
            sb.append("### 🌟 修飾後英文全文 (Refined English Text)\n\n");

            JsonNode refinedText = root.path("refinedText");
            String casual = refinedText.path("casual").asText("");
            if (casual.isBlank()) casual = root.path("refined_casual").asText("");

            String formal = refinedText.path("formal").asText("");
            if (formal.isBlank()) formal = root.path("refined_formal").asText("");

            sb.append("🗣️ **自然道地口語版 (Natural & Conversational)**\n");
            sb.append("> ").append(casual.replace("\n", "\n> ")).append("\n\n");

            sb.append("💼 **專業商務正式版 (Professional & Formal)**\n");
            sb.append("> ").append(formal.replace("\n", "\n> ")).append("\n\n");

            sb.append("---\n\n");

            // 2. 🔍 語病與道地性解析
            JsonNode grammarAnalysis = root.path("grammarAnalysis");
            if (grammarAnalysis.isMissingNode()) grammarAnalysis = root.path("grammar_analysis");
            if (grammarAnalysis.isArray() && !grammarAnalysis.isEmpty()) {
                sb.append("### 🔍 語病與道地性解析 (Chinglish & Grammar Analysis)\n");
                for (JsonNode item : grammarAnalysis) {
                    String original = item.path("original").asText("");
                    String explanation = item.path("explanation").asText("");
                    if (!original.isBlank() || !explanation.isBlank()) {
                        sb.append("- **").append(original).append("** ➔ ").append(explanation).append("\n");
                    }
                }
                sb.append("\n");
            }

            // 3. 💡 修改原因與語境解析
            JsonNode rationale = root.path("rationale");
            if (rationale.isArray() && !rationale.isEmpty()) {
                sb.append("### 💡 修改原因與語境解析 (Rationale & Insights)\n");
                for (JsonNode item : rationale) {
                    String text = item.isTextual() ? item.asText() : item.path("text").asText("");
                    if (!text.isBlank()) {
                        sb.append("- ").append(text).append("\n");
                    }
                }
                sb.append("\n");
            }

            // 4. 📚 實用道地片語與延伸替換
            JsonNode idioms = root.path("idiomsAndUpgrades");
            if (idioms.isMissingNode()) idioms = root.path("idioms_and_upgrades");
            if (idioms.isArray() && !idioms.isEmpty()) {
                sb.append("### 📚 實用道地片語與延伸替換 (Idioms & Upgrades)\n");
                for (JsonNode item : idioms) {
                    String phrase = item.path("phrase").asText("");
                    String meaning = item.path("meaning").asText("");
                    String example = item.path("example").asText("");
                    if (!phrase.isBlank()) {
                        sb.append("- **").append(phrase).append("**：").append(meaning).append("\n");
                        if (!example.isBlank()) {
                            sb.append("  - *例句*：").append(example).append("\n");
                        }
                    }
                }
            }

            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("解析修飾結果 JSON 失敗，回退為原始輸出: {}", e.getMessage());
            return jsonOrText;
        }
    }

    /**
     * 追加發問 (Follow-up) 的 System Prompt。
     * 規範以 JSON 格式輸出 reply 欄位。
     */
    public String getFollowUpSystemPrompt() {
        return """
            你是一位高效、精準的英語母語專家寫作教練。
            請以繁體中文與道地英文解答使用者的追加疑問。
            【約束】：
            1. 必須嚴格以合法 JSON 格式輸出：{"reply": "解答內容"}，不得夾帶 ```json 標籤。
            2. "reply" 欄位值中可自由使用標準 Markdown 排版（如粗體、條列、引號 blockquote、程式碼等）。
            3. 嚴禁任何開場問候與客套廢話。
            """;
    }

    /**
     * 後端配套措施：處理追加發問的 JSON 回覆
     */
    public String formatFollowUpJsonToText(String jsonOrText) {
        if (jsonOrText == null || jsonOrText.isBlank()) {
            return "";
        }
        String trimmed = jsonOrText.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();

        if (trimmed.startsWith("{")) {
            try {
                JsonNode root = objectMapper.readTree(trimmed);
                if (root.has("reply")) {
                    return root.path("reply").asText();
                }
                if (root.has("text")) {
                    return root.path("text").asText();
                }
                if (root.has("response")) {
                    return root.path("response").asText();
                }
                if (root.has("answer")) {
                    return root.path("answer").asText();
                }
            } catch (Exception ignored) {}
        }
        return jsonOrText;
    }
}
