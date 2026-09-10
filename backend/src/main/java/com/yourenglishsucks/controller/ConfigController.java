package com.yourenglishsucks.controller;

import com.yourenglishsucks.service.GeminiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ConfigController {

    private final GeminiService geminiService;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String modelName;

    public ConfigController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of(
                "hasBackendApiKey", geminiService.hasConfiguredApiKey(),
                "model", modelName,
                "status", "ONLINE"
        ));
    }
}
