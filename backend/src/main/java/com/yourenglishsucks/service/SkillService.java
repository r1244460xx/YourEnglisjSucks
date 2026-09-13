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
     * 包含任務目標、改寫準則（casual/formal）與少樣本範例 (Few-Shot Examples)。
     */
    public String getEnglishPolishSystemPrompt() {
        return """
            你是一位高效、精準、直奔重點的資深英語母語編輯與寫作教練。

            【任務目標】：
            針對使用者輸入的 100 字以內英文，深度分析其文法、時態與中式直譯盲點。提供「道地口語（Casual）」與「專業正式（Formal）」兩種維度的改寫，並給予精準的語法思維解析與高級片語升級建議。

            【改寫準則】：
            - casual：模擬北美母語人士日常聊天、傳訊時自然脫口而出的表達，善用動詞片語（Phrasal Verbs），消除生硬感。
            - formal：適合商業書信、職場溝通或學術寫作，用詞精準洗鍊、結構嚴謹。
            - 嚴格保留原句的核心語意，不得隨意擴寫無關情節。

            【少樣本範例 (Few-Shot Examples)】：

            [Example 1]
            Input: "I very like to drink coffee in morning because it makes me spirit."
            Output:
            {
              "refinedText": {
                "casual": "I'm a big coffee drinker, especially in the morning—it really gets me going.",
                "formal": "I regularly drink coffee in the morning to stay energized throughout the day."
              },
              "grammarAnalysis": [
                {
                  "original": "very like",
                  "explanation": "'very' 不能直接修飾動詞 'like'，此為典型的中式直譯，應改用 'really like' 或慣用句型 'be a big fan of'。"
                },
                {
                  "original": "in morning",
                  "explanation": "時間副詞缺少定冠詞，應修正為 'in the morning'。"
                },
                {
                  "original": "makes me spirit",
                  "explanation": "'spirit' 是名詞，無法作形容詞表狀態。應使用使役動詞片語或改用 'energizes me'、'keeps me alert'。"
                }
              ],
              "rationale": [
                "中文常說『很有精神』，但在英文思維中通常從動能（get going）或能量狀態（stay energized）切入，而非直接翻譯名詞 spirit。",
                "口語中母語人士習慣以身分傾向句型（I'm a big coffee drinker）代替單純的偏好描述（I like coffee）。"
              ],
              "idiomsAndUpgrades": [
                {
                  "phrase": "get someone going",
                  "meaning": "使某人精力充沛、進入工作或活動狀態",
                  "example": "A strong cup of espresso always gets me going. (一杯濃縮咖啡總能讓我精神百倍。)"
                },
                {
                  "phrase": "kick-start one's day",
                  "meaning": "展開充實/活力滿滿的一天",
                  "example": "I need a workout to kick-start my day. (我需要透過運動來開啟我的一天。)"
                }
              ]
            }

            [Example 2]
            Input: "I will contact with you tomorrow about the project process."
            Output:
            {
              "refinedText": {
                "casual": "I'll touch base with you tomorrow on how the project is coming along.",
                "formal": "I will follow up with you tomorrow regarding the progress of the project."
              },
              "grammarAnalysis": [
                {
                  "original": "contact with you",
                  "explanation": "'contact' 在此作為及物動詞，後面直接接受詞，不需要加介系詞 'with'。"
                },
                {
                  "original": "project process",
                  "explanation": "'process' 指的是流程或工序，若表達進展或進度，應使用 'progress' 或狀態片語。"
                }
              ],
              "rationale": [
                "職場口語中，'touch base' 比單純的 'contact' 更具互動感與母語味；商務書信則推薦使用 'follow up regarding'。",
                "英文中『進度』與『流程』語意切分嚴格，形容專案狀態時多用 progress。"
              ],
              "idiomsAndUpgrades": [
                {
                  "phrase": "touch base (with)",
                  "meaning": "與某人短暫聯繫、交換意見或確認最新進度",
                  "example": "Let's touch base next Monday to review the design. (我們下週一碰一下確認設計案。)"
                },
                {
                  "phrase": "come along",
                  "meaning": "進展、進行順利",
                  "example": "How is the new feature coming along? (那個新功能的開發進度如何？)"
                }
              ]
            }
            """;
    }

    /**
     * 提供 Gemini API 專用的 responseSchema 定義。
     * 強制模型遵循包含 refinedText, grammarAnalysis, rationale, idiomsAndUpgrades 的結構化格式。
     */
    public java.util.Map<String, Object> getPolishResponseSchema() {
        return java.util.Map.of(
            "type", "OBJECT",
            "properties", java.util.Map.of(
                "refinedText", java.util.Map.of(
                    "type", "OBJECT",
                    "properties", java.util.Map.of(
                        "casual", java.util.Map.of("type", "STRING", "description", "自然道地口語版完整英文句子或段落"),
                        "formal", java.util.Map.of("type", "STRING", "description", "專業商務正式版完整英文句子或段落")
                    ),
                    "required", java.util.List.of("casual", "formal")
                ),
                "grammarAnalysis", java.util.Map.of(
                    "type", "ARRAY",
                    "description", "文法與時態問題清單",
                    "items", java.util.Map.of(
                        "type", "OBJECT",
                        "properties", java.util.Map.of(
                            "original", java.util.Map.of("type", "STRING", "description", "引述原文有問題的字句"),
                            "explanation", java.util.Map.of("type", "STRING", "description", "說明文法錯誤、時態問題、中式英文或不道地之處")
                        ),
                        "required", java.util.List.of("original", "explanation")
                    )
                ),
                "rationale", java.util.Map.of(
                    "type", "ARRAY",
                    "description", "背後英語思維與語境差異說明",
                    "items", java.util.Map.of("type", "STRING")
                ),
                "idiomsAndUpgrades", java.util.Map.of(
                    "type", "ARRAY",
                    "description", "推薦的道地片語與詞彙升級清單",
                    "items", java.util.Map.of(
                        "type", "OBJECT",
                        "properties", java.util.Map.of(
                            "phrase", java.util.Map.of("type", "STRING", "description", "片語或詞彙"),
                            "meaning", java.util.Map.of("type", "STRING", "description", "中文意義與用法說明"),
                            "example", java.util.Map.of("type", "STRING", "description", "英文例句 (中文翻譯)")
                        ),
                        "required", java.util.List.of("phrase", "meaning", "example")
                    )
                )
            ),
            "required", java.util.List.of("refinedText", "grammarAnalysis", "rationale", "idiomsAndUpgrades")
        );
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
        return formatPolishJsonToMarkdown(jsonOrText, null);
    }

    /**
     * 後端配套措施：將 Gemini 回傳的嚴格 JSON 結構轉換為標準 Markdown 排版。
     * 確保前端依然擁有：
     * 1. 頂部獨立 > blockquote 引用區塊（包含修改前原始草稿，以及自然道地口語版與專業商務正式版並列對照）。
     * 2. 乾淨清爽的四階段條列分析。
     * 3. 具備容錯機制，若非標準 JSON 則平滑回退為原文字串。
     */
    public String formatPolishJsonToMarkdown(String jsonOrText, String rawEnglishText) {
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
            if (rawEnglishText != null && !rawEnglishText.isBlank() && !jsonOrText.contains("修改前英文原文本")) {
                return "### 📝 修改前英文原文本 (Original Draft)\n> " +
                        rawEnglishText.trim().replace("\n", "\n> ") + "\n\n---\n\n" + jsonOrText;
            }
            return jsonOrText;
        }

        try {
            JsonNode root = objectMapper.readTree(trimmed);
            StringBuilder sb = new StringBuilder();

            // 0. 📝 修改前英文原文本 (Original Draft)
            if (rawEnglishText != null && !rawEnglishText.isBlank()) {
                sb.append("### 📝 修改前英文原文本 (Original Draft)\n");
                sb.append("> ").append(rawEnglishText.trim().replace("\n", "\n> ")).append("\n\n");
                sb.append("---\n\n");
            }

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
     * 追加發問 (Follow-up) 的 responseSchema。
     * 強制要求 Gemini 輸出包含 reply 欄位的 JSON 物件。
     */
    public java.util.Map<String, Object> getFollowUpResponseSchema() {
        return java.util.Map.of(
            "type", "OBJECT",
            "properties", java.util.Map.of(
                "reply", java.util.Map.of("type", "STRING", "description", "針對追加問題的繁體中文與道地英文解答內容")
            ),
            "required", java.util.List.of("reply")
        );
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
