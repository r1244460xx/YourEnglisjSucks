package com.yourenglishsucks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FollowUpRequest(
    @NotBlank(message = "追加訊息不能為空")
    @Size(max = 1000, message = "追加訊息最多為 1000 個字元")
    String message,
    String apiKey
) {}
