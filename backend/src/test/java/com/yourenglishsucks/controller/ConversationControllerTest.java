package com.yourenglishsucks.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourenglishsucks.dto.FollowUpRequest;
import com.yourenglishsucks.dto.PolishRequest;
import com.yourenglishsucks.dto.UpdateTitleRequest;
import com.yourenglishsucks.entity.PolishRawSubmission;
import com.yourenglishsucks.repository.ChatMessageRepository;
import com.yourenglishsucks.repository.ConversationRepository;
import com.yourenglishsucks.repository.PolishRawSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private PolishRawSubmissionRepository polishRawSubmissionRepository;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();
        polishRawSubmissionRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    @Test
    @DisplayName("Use Case 1: 首次修飾文本與獨立資料表持久化，接續同 Session 追加發問")
    void testStartPolishAndFollowUpFlow() throws Exception {
        // 1. 首次修飾文本
        String rawEnglish = "I am write this for update project status because we have error.";
        PolishRequest polishRequest = new PolishRequest(rawEnglish, null);

        MvcResult polishResult = mockMvc.perform(post("/api/conversations/polish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(polishRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversation.id").isNotEmpty())
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].senderType").value("USER"))
                .andExpect(jsonPath("$.messages[0].roundNumber").value(1))
                .andExpect(jsonPath("$.messages[1].senderType").value("AI"))
                .andExpect(jsonPath("$.messages[1].roundNumber").value(1))
                .andReturn();

        String responseBody = polishResult.getResponse().getContentAsString();
        String convIdStr = objectMapper.readTree(responseBody).path("conversation").path("id").asText();
        UUID conversationId = UUID.fromString(convIdStr);

        // 2. 驗證獨立 table (polish_raw_submissions) 有正確記錄第一次的原始文本
        Optional<PolishRawSubmission> rawSubmission = polishRawSubmissionRepository.findByConversationId(conversationId);
        assertThat(rawSubmission).isPresent();
        assertThat(rawSubmission.get().getRawText()).isEqualTo(rawEnglish);
        assertThat(rawSubmission.get().getWordCount()).isGreaterThan(0);

        // 3. 追加提問 (Follow-up) - 同 Session 討論
        FollowUpRequest followUpRequest = new FollowUpRequest("可以更口語一點嗎？", null);
        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(followUpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderType").value("AI"))
                .andExpect(jsonPath("$.roundNumber").value(2));

        // 4. 取得該對話所有訊息 (一次性載入該 Session 全部對話)
        mockMvc.perform(get("/api/conversations/" + conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(4));
    }

    @Test
    @DisplayName("Use Case 2: 手動重新命名對話標題 (Rename Title)")
    void testRenameConversationTitle() throws Exception {
        // 先建立對話
        PolishRequest polishRequest = new PolishRequest("Initial draft text for testing rename.", null);
        MvcResult result = mockMvc.perform(post("/api/conversations/polish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(polishRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String convIdStr = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("conversation").path("id").asText();

        // 重新命名
        UpdateTitleRequest updateTitleRequest = new UpdateTitleRequest("外商主管進度更新信");
        mockMvc.perform(patch("/api/conversations/" + convIdStr + "/title")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateTitleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("外商主管進度更新信"));

        // 驗證查詢該對話詳情時標題確實更新
        mockMvc.perform(get("/api/conversations/" + convIdStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversation.title").value("外商主管進度更新信"));
    }

    @Test
    @DisplayName("Use Case 3: 一次性載入全部歷史對話與刪除操作")
    void testGetAllConversationsAndDelete() throws Exception {
        // 建立兩筆對話
        mockMvc.perform(post("/api/conversations/polish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PolishRequest("First draft", null))))
                .andExpect(status().isOk());

        MvcResult secondResult = mockMvc.perform(post("/api/conversations/polish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new PolishRequest("Second draft", null))))
                .andExpect(status().isOk())
                .andReturn();

        String secondIdStr = objectMapper.readTree(secondResult.getResponse().getContentAsString())
                .path("conversation").path("id").asText();

        // 驗證一次載入全部 (長度為 2)
        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // 刪除第二筆對話
        mockMvc.perform(delete("/api/conversations/" + secondIdStr))
                .andExpect(status().isNoContent());

        // 再次查詢，只剩一筆未封存
        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Use Case 4: 輸入文本超過 1000 字元限制時應被拒絕 (400 Bad Request)")
    void testMaxLengthValidation() throws Exception {
        String overlyLongText = "a".repeat(1001);
        PolishRequest request = new PolishRequest(overlyLongText, null);

        mockMvc.perform(post("/api/conversations/polish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Use Case 5: 輸入空白或只有空白字元時應被拒絕 (400 Bad Request)")
    void testBlankInputValidation() throws Exception {
        PolishRequest request = new PolishRequest("    ", null);

        mockMvc.perform(post("/api/conversations/polish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Use Case 6: 驗證首次修飾文本端到端真流式輸出 (SSE Stream)")
    void testStartPolishStreamFlow() throws Exception {
        PolishRequest request = new PolishRequest("I am write this to test streaming output.", null);

        MvcResult result = mockMvc.perform(post("/api/conversations/polish/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // 驗證回傳 Content-Type 為 text/event-stream
        String contentType = result.getResponse().getContentType();
        assertThat(contentType).contains(MediaType.TEXT_EVENT_STREAM_VALUE);

        // 驗證獨立 table (polish_raw_submissions) 正確持久化
        assertThat(polishRawSubmissionRepository.count()).isEqualTo(1);
    }
}
