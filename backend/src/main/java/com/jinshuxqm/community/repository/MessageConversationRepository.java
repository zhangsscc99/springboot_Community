package com.jinshuxqm.community.repository;

import com.jinshuxqm.community.model.MessageConversation;
import com.jinshuxqm.community.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageConversationRepository extends JpaRepository<MessageConversation, Long> {
    
    // 查找两个用户之间的会话
    @Query("SELECT mc FROM MessageConversation mc WHERE " +
           "(mc.user1 = :user1 AND mc.user2 = :user2) OR " +
           "(mc.user1 = :user2 AND mc.user2 = :user1)")
    Optional<MessageConversation> findConversationBetweenUsers(
            @Param("user1") User user1,
            @Param("user2") User user2);
    
    // 查找用户参与的所有会话
    @Query("SELECT mc FROM MessageConversation mc WHERE " +
           "mc.user1 = :user OR mc.user2 = :user " +
           "ORDER BY mc.lastMessageTime DESC")
    Page<MessageConversation> findConversationsForUser(
            @Param("user") User user,
            Pageable pageable);
} 