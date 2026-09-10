package com.yourenglishsucks.dto;

import jakarta.validation.constraints.NotBlank;

public record FollowUpRequest(
    @NotBlank(message = "追加訊息不能為空")
    String message,
    String apiKey
) {}
