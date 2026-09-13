package com.yourenglishsucks.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillServiceTest {

    private SkillService skillService;

    @BeforeEach
    void setUp() {
        skillService = new SkillService(new ObjectMapper());
    }

    @Test
    @DisplayName("驗證標準 JSON 能成功轉換為帶有 blockquote 與條列說明的標準 Markdown")
    void testFormatPolishJsonToMarkdownSuccess() {
        String json = """
            {
              "refinedText": {
                "casual": "I'm writing to let you know about the project status.",
                "formal": "I am writing to provide an update on our project schedule."
              },
              "grammarAnalysis": [
                {
                  "original": "for update",
                  "explanation": "應使用 to update。"
                }
              ],
              "rationale": [
                "商務語境中更加客氣專業。"
              ],
              "idiomsAndUpgrades": [
                {
                  "phrase": "hit a snag",
                  "meaning": "遇到阻礙",
                  "example": "We hit a snag."
                }
              ]
            }
            """;

        String markdown = skillService.formatPolishJsonToMarkdown(json);

        assertThat(markdown).contains("### 🌟 修飾後英文全文 (Refined English Text)");
        assertThat(markdown).contains("> I'm writing to let you know about the project status.");
        assertThat(markdown).contains("> I am writing to provide an update on our project schedule.");
        assertThat(markdown).contains("### 🔍 語病與道地性解析 (Chinglish & Grammar Analysis)");
        assertThat(markdown).contains("- **for update** ➔ 應使用 to update。");
        assertThat(markdown).contains("### 💡 修改原因與語境解析 (Rationale & Insights)");
        assertThat(markdown).contains("- 商務語境中更加客氣專業。");
        assertThat(markdown).contains("### 📚 實用道地片語與延伸替換 (Idioms & Upgrades)");
        assertThat(markdown).contains("- **hit a snag**：遇到阻礙");
        assertThat(markdown).contains("  - *例句*：We hit a snag.");
    }

    @Test
    @DisplayName("驗證當輸出包含 ```json 標籤包裹時仍能正常解析")
    void testFormatPolishJsonWithCodeBlockTags() {
        String wrappedJson = """
            ```json
            {
              "refinedText": {
                "casual": "Here is the update.",
                "formal": "Please find the status update attached."
              }
            }
            ```
            """;

        String markdown = skillService.formatPolishJsonToMarkdown(wrappedJson);

        assertThat(markdown).contains("> Here is the update.");
        assertThat(markdown).contains("> Please find the status update attached.");
    }

    @Test
    @DisplayName("驗證當非 JSON 格式輸入時平滑回退為原始字串")
    void testFormatPolishJsonFallback() {
        String rawText = "This is a plain text response without JSON structure.";
        String result = skillService.formatPolishJsonToMarkdown(rawText);
        assertThat(result).isEqualTo(rawText);
    }

    @Test
    @DisplayName("驗證追加提問 JSON 能正確提取 reply 欄位")
    void testFormatFollowUpJsonToText() {
        String json = "{\"reply\": \"這是一個道地的口語說法。\"}";
        String text = skillService.formatFollowUpJsonToText(json);
        assertThat(text).isEqualTo("這是一個道地的口語說法。");
    }
}
