package com.hotpulse.repository;

import com.hotpulse.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Page<Conversation> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
