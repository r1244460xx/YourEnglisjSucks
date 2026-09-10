package com.yourenglishsucks.service;

import com.yourenglishsucks.dto.ChatMessageResponse;
import com.yourenglishsucks.dto.ConversationDetailResponse;
import com.yourenglishsucks.dto.ConversationResponse;
import com.yourenglishsucks.dto.FollowUpRequest;
import com.yourenglishsucks.dto.PageResponse;
import com.yourenglishsucks.dto.PolishRequest;
import com.yourenglishsucks.entity.ChatMessage;
import com.yourenglishsucks.entity.Conversation;
import com.yourenglishsucks.entity.PolishRawSubmission;
import com.yourenglishsucks.repository.ChatMessageRepository;
import com.yourenglishsucks.repository.ConversationRepository;
import com.yourenglishsucks.repository.PolishRawSubmissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PolishRawSubmissionRepository polishRawSubmissionRepository;
    private final GeminiService geminiService;
    private final SkillService skillService;

    public ConversationService(
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository,
            PolishRawSubmissionRepository polishRawSubmissionRepository,
            GeminiService geminiService,
            SkillService skillService) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.polishRawSubmissionRepository = polishRawSubmissionRepository;
        this.geminiService = geminiService;
        this.skillService = skillService;
    }

    /**
     * 首次送出英文修飾文本：
     * 1. 建立 Conversation
     * 2. 存入獨立 table (polish_raw_submissions)
     * 3. 存入使用者第 1 輪訊息 (chat_messages)
     * 4. 搭配專屬英文修飾 Skill 發送 Gemini
     * 5. 存入 AI 第 1 輪訊息
     */
    @Transactional
    public ConversationDetailResponse startPolish(PolishRequest request) {
        String rawText = request.rawText().trim();
        if (rawText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "輸入的英文文本不能為空");
        }

        // 產生標題：取首段前 40 字元
        String title = generateTitle(rawText);

        // 1. 建立 Conversation
        Conversation conversation = new Conversation(title, "POLISH");
        conversation = conversationRepository.save(conversation);

        // 2. 存入獨立分析表 polish_raw_submissions
        PolishRawSubmission submission = new PolishRawSubmission(conversation.getId(), rawText);
        polishRawSubmissionRepository.save(submission);

        // 3. 儲存使用者第 1 輪訊息
        ChatMessage userMessage = new ChatMessage(conversation.getId(), "USER", rawText, 1);
        userMessage = chatMessageRepository.save(userMessage);

        // 4. 搭配專屬 Skill 呼叫 Gemini
        String systemPrompt = skillService.getEnglishPolishSystemPrompt();
        String userPrompt = skillService.buildFirstRoundPolishPrompt(rawText);
        List<Map<String, String>> history = List.of(
                Map.of("role", "user", "text", userPrompt)
        );

        String aiReply = geminiService.generateContent(systemPrompt, history, request.apiKey());

        // 5. 儲存 AI 第 1 輪訊息
        ChatMessage aiMessage = new ChatMessage(conversation.getId(), "AI", aiReply, 1);
        aiMessage = chatMessageRepository.save(aiMessage);

        // 回傳完整對話物件
        List<ChatMessageResponse> messages = List.of(
                mapMessageToResponse(userMessage),
                mapMessageToResponse(aiMessage)
        );

        return new ConversationDetailResponse(
                mapConversationToResponse(conversation),
                messages,
                rawText
        );
    }

    /**
     * 針對同一對話追加發問 (Follow-up)：
     * 1. 驗證對話存在
     * 2. 存入使用者追加發問訊息 (round N)
     * 3. 組合歷史上下文 (不帶 Polish Skill，直接發給 AI)
     * 4. 存入 AI 回覆 (round N)
     */
    @Transactional
    public ChatMessageResponse addFollowUp(UUID conversationId, FollowUpRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .filter(c -> !c.isArchived())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "對話不存在或已被刪除"));

        String userQuery = request.message().trim();
        if (userQuery.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "追加訊息不能為空");
        }

        // 計算目前輪次
        long count = chatMessageRepository.countByConversationId(conversationId);
        int currentRound = (int) (count / 2) + 1;

        // 儲存使用者追加發問
        ChatMessage userMsg = new ChatMessage(conversationId, "USER", userQuery, currentRound);
        chatMessageRepository.save(userMsg);

        // 組合歷史對話清單 (不帶 Polish Skill, 保留所有歷史訊息)
        List<ChatMessage> allMessages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<Map<String, String>> history = new ArrayList<>();
        for (ChatMessage msg : allMessages) {
            String role = "USER".equalsIgnoreCase(msg.getSenderType()) ? "user" : "model";
            history.add(Map.of("role", role, "text", msg.getContent()));
        }

        // 呼叫 Gemini (systemInstruction 為 null，標準 Chat 模式)
        String aiReply = geminiService.generateContent(null, history, request.apiKey());

        // 儲存 AI 回覆
        ChatMessage aiMsg = new ChatMessage(conversationId, "AI", aiReply, currentRound);
        aiMsg = chatMessageRepository.save(aiMsg);

        // 更新對話最後活躍時間
        conversation.setUpdatedAt(aiMsg.getCreatedAt());
        conversationRepository.save(conversation);

        return mapMessageToResponse(aiMsg);
    }

    /**
     * 一次性載入全部歷史對話清單
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> getAllConversations() {
        return conversationRepository.findByArchivedFalseOrderByUpdatedAtDesc().stream()
                .map(this::mapConversationToResponse)
                .toList();
    }

    /**
     * 分頁取得歷史對話清單
     */
    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> getConversations(Pageable pageable) {
        Page<Conversation> page = conversationRepository.findByArchivedFalseOrderByUpdatedAtDesc(pageable);
        List<ConversationResponse> items = page.getContent().stream()
                .map(this::mapConversationToResponse)
                .toList();

        return new PageResponse<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }

    /**
     * 取得特定對話的完整內容與歷史訊息
     */
    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversationDetails(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .filter(c -> !c.isArchived())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "對話不存在"));

        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<ChatMessageResponse> messageResponses = messages.stream()
                .map(this::mapMessageToResponse)
                .toList();

        String rawText = polishRawSubmissionRepository.findByConversationId(conversationId)
                .map(PolishRawSubmission::getRawText)
                .orElse("");

        return new ConversationDetailResponse(
                mapConversationToResponse(conversation),
                messageResponses,
                rawText
        );
    }

    @Transactional
    public ConversationResponse updateTitle(UUID conversationId, String newTitle) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .filter(c -> !c.isArchived())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "對話不存在或已被刪除"));
        conversation.setTitle(newTitle.trim());
        conversation = conversationRepository.save(conversation);
        return mapConversationToResponse(conversation);
    }

    @Transactional
    public void deleteConversation(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "對話不存在"));
        conversation.setArchived(true);
        conversationRepository.save(conversation);
    }

    private String generateTitle(String text) {
        String clean = text.replace("\r", " ").replace("\n", " ").trim();
        if (clean.length() <= 35) {
            return clean;
        }
        return clean.substring(0, 35) + "...";
    }

    private ConversationResponse mapConversationToResponse(Conversation c) {
        return new ConversationResponse(
                c.getId(),
                c.getTitle(),
                c.getMode(),
                c.isArchived(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    private ChatMessageResponse mapMessageToResponse(ChatMessage m) {
        return new ChatMessageResponse(
                m.getId(),
                m.getConversationId(),
                m.getSenderType(),
                m.getContent(),
                m.getRoundNumber(),
                m.getMetadata(),
                m.getCreatedAt()
        );
    }
}
