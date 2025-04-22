package com.jinshuxqm.community.repository;

import com.jinshuxqm.community.model.PrivateMessage;
import com.jinshuxqm.community.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {
    
    // 获取两个用户之间的所有消息（分页）
    @Query("SELECT pm FROM PrivateMessage pm WHERE " +
           "(pm.sender = :user1 AND pm.receiver = :user2) OR " +
           "(pm.sender = :user2 AND pm.receiver = :user1) " +
           "ORDER BY pm.createdAt ASC")
    Page<PrivateMessage> findMessagesBetweenUsers(
            @Param("user1") User user1,
            @Param("user2") User user2,
            Pageable pageable);
    
    // 获取用户的所有未读消息
    List<PrivateMessage> findByReceiverAndReadFalse(User receiver);
    
    // 获取指定用户之间的未读消息
    List<PrivateMessage> findBySenderAndReceiverAndReadFalse(User sender, User receiver);
    
    // 批量将消息标记为已读
    @Modifying
    @Query("UPDATE PrivateMessage pm SET pm.read = true WHERE pm.id IN :ids")
    void markMessagesAsRead(@Param("ids") List<Long> messageIds);
    
    // 获取两个用户之间的最后一条消息
    @Query("SELECT pm FROM PrivateMessage pm WHERE " +
           "(pm.sender = :user1 AND pm.receiver = :user2) OR " +
           "(pm.sender = :user2 AND pm.receiver = :user1) " +
           "ORDER BY pm.createdAt DESC")
    List<PrivateMessage> findLatestMessageBetweenUsers(
            @Param("user1") User user1,
            @Param("user2") User user2,
            Pageable pageable);
} 