package com.yourenglishsucks.dto;

import java.util.List;

public record ConversationDetailResponse(
    ConversationResponse conversation,
    List<ChatMessageResponse> messages,
    String rawSubmissionText
) {}
