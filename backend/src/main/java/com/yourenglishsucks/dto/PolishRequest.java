package com.yourenglishsucks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PolishRequest(
    @NotBlank(message = "英文文本不能為空")
    @Size(max = 1000, message = "英文文本最多為 1000 個字元")
    String rawText,
    String apiKey
) {}
