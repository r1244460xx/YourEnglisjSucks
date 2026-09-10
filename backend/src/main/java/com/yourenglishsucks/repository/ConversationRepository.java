package com.yourenglishsucks.repository;

import com.yourenglishsucks.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findByArchivedFalseOrderByUpdatedAtDesc();
    Page<Conversation> findByArchivedFalseOrderByUpdatedAtDesc(Pageable pageable);
    Page<Conversation> findByModeAndArchivedFalseOrderByUpdatedAtDesc(String mode, Pageable pageable);
}
