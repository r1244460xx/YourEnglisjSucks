package com.yourenglishsucks.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
    UUID id,
    String title,
    String mode,
    boolean archived,
    Instant createdAt,
    Instant updatedAt
) {}
