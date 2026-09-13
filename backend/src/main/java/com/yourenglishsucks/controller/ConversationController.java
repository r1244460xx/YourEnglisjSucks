package com.yourenglishsucks.controller;

import com.yourenglishsucks.dto.ChatMessageResponse;
import com.yourenglishsucks.dto.ConversationDetailResponse;
import com.yourenglishsucks.dto.ConversationResponse;
import com.yourenglishsucks.dto.FollowUpRequest;
import com.yourenglishsucks.dto.PageResponse;
import com.yourenglishsucks.dto.PolishRequest;
import com.yourenglishsucks.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * 第一次提交英文文本修飾
     */
    @PostMapping("/polish")
    public ResponseEntity<ConversationDetailResponse> startPolish(@Valid @RequestBody PolishRequest request) {
        ConversationDetailResponse response = conversationService.startPolish(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 第一次提交英文文本修飾 (端到端真流式輸出 SSE)
     */
    @PostMapping(value = "/polish/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter startPolishStream(
            @Valid @RequestBody PolishRequest request) {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter =
                new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(180_000L);
        conversationService.startPolishStream(request, emitter);
        return emitter;
    }

    /**
     * 在同一對話中追加發問 (Follow-up)
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<ChatMessageResponse> addFollowUp(
            @PathVariable("id") UUID id,
            @Valid @RequestBody FollowUpRequest request) {
        ChatMessageResponse response = conversationService.addFollowUp(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 在同一對話中追加發問 (端到端真流式輸出 SSE)
     */
    @PostMapping(value = "/{id}/messages/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter addFollowUpStream(
            @PathVariable("id") UUID id,
            @Valid @RequestBody FollowUpRequest request) {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter =
                new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(180_000L);
        conversationService.addFollowUpStream(id, request, emitter);
        return emitter;
    }

    /**
     * 取得歷史對話 (預設一次載入全部；亦支援 ?paged=true 進行分頁)
     */
    @GetMapping
    public ResponseEntity<?> getConversations(
            @RequestParam(name = "paged", defaultValue = "false") boolean paged,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "15") int size) {
        if (paged) {
            return ResponseEntity.ok(conversationService.getConversations(PageRequest.of(page, size)));
        }
        return ResponseEntity.ok(conversationService.getAllConversations());
    }

    /**
     * 取得特定對話所有內容
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConversationDetailResponse> getConversationDetails(@PathVariable("id") UUID id) {
        ConversationDetailResponse response = conversationService.getConversationDetails(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 手動修改對話標題 (Rename Title)
     */
    @org.springframework.web.bind.annotation.PatchMapping("/{id}/title")
    public ResponseEntity<ConversationResponse> updateTitle(
            @PathVariable("id") UUID id,
            @Valid @RequestBody com.yourenglishsucks.dto.UpdateTitleRequest request) {
        ConversationResponse response = conversationService.updateTitle(id, request.title());
        return ResponseEntity.ok(response);
    }

    /**
     * 刪除或封存對話
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable("id") UUID id) {
        conversationService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }
}
