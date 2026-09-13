package com.yourenglishsucks.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("業務狀態例外 (HTTP {}): {}", ex.getStatusCode(), ex.getReason());
        Map<String, Object> body = buildErrorBody(
                ex.getStatusCode().value(),
                ex.getStatusCode().toString(),
                ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                ex.getMessage()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        String rawError = ex.getMessage();

        log.warn("輸入參數驗證失敗: {}", details);
        Map<String, Object> body = buildErrorBody(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request (Validation Failed)",
                details.isBlank() ? "輸入參數格式有誤" : details,
                rawError
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleRestClientResponseException(RestClientResponseException ex) {
        String responseBody = ex.getResponseBodyAsString();
        String message = "上游服務呼叫失敗 (HTTP " + ex.getStatusCode() + "): " +
                (responseBody.isBlank() ? ex.getMessage() : responseBody);

        log.error("上游 API 回傳錯誤: HTTP {} - {}", ex.getStatusCode(), responseBody, ex);
        Map<String, Object> body = buildErrorBody(
                ex.getStatusCode().value(),
                "Upstream API Error",
                message,
                ex.toString()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("非法參數例外: {}", ex.getMessage());
        Map<String, Object> body = buildErrorBody(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request (Illegal Argument)",
                ex.getMessage(),
                ex.toString()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Throwable ex) {
        log.error("系統未預期例外", ex);

        // 取得最底層 Root Cause
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        String rawMessage = root.getMessage() != null && !root.getMessage().isBlank()
                ? root.getMessage()
                : ex.getMessage();
        if (rawMessage == null || rawMessage.isBlank()) {
            rawMessage = ex.getClass().getSimpleName();
        }

        Map<String, Object> body = buildErrorBody(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getClass().getSimpleName(),
                rawMessage,
                ex.toString()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Map<String, Object> buildErrorBody(int status, String error, String message, String rawError) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("rawError", rawError);
        return body;
    }
}
