package com.yourenglishsucks.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
    UUID id,
    UUID conversationId,
    String senderType,
    String content,
    int roundNumber,
    String metadata,
    Instant createdAt
) {}
