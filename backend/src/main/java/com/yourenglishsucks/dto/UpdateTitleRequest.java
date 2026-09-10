package com.yourenglishsucks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTitleRequest(
    @NotBlank(message = "對話標題不能為空")
    @Size(max = 100, message = "標題長度最多 100 個字元")
    String title
) {}
