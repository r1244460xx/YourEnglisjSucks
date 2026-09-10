package com.yourenglishsucks.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 專為分析使用者寫作與英文使用習慣所設立的獨立資料表。
 * 僅記錄每次新對話中「第一次送出的原始英文文本」。
 */
@Entity
@Table(name = "polish_raw_submissions")
public class PolishRawSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID conversationId;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(nullable = false)
    private int charCount;

    @Column(nullable = false)
    private int wordCount;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String habitFeatures; // 供未來擴充紀錄文法分析、常用單字、錯誤標籤等特徵 (JSON)

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public PolishRawSubmission() {}

    public PolishRawSubmission(UUID conversationId, String rawText) {
        this.conversationId = conversationId;
        this.rawText = rawText;
        this.charCount = rawText != null ? rawText.length() : 0;
        this.wordCount = calculateWordCount(rawText);
    }

    private static int calculateWordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String[] words = text.trim().split("\\s+");
        return words.length;
    }

    @PrePersist
    public void onPrePersist() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
        this.charCount = rawText != null ? rawText.length() : 0;
        this.wordCount = calculateWordCount(rawText);
    }

    public int getCharCount() {
        return charCount;
    }

    public void setCharCount(int charCount) {
        this.charCount = charCount;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public String getHabitFeatures() {
        return habitFeatures;
    }

    public void setHabitFeatures(String habitFeatures) {
        this.habitFeatures = habitFeatures;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
