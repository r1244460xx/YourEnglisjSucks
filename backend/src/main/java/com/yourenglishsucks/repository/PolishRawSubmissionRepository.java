package com.yourenglishsucks.repository;

import com.yourenglishsucks.entity.PolishRawSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PolishRawSubmissionRepository extends JpaRepository<PolishRawSubmission, UUID> {
    Optional<PolishRawSubmission> findByConversationId(UUID conversationId);
    void deleteByConversationId(UUID conversationId);
}
