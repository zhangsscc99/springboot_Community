package com.jinshuxqm.community.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "message_conversations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user1_id", "user2_id"}))
public class MessageConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_message_id")
    private PrivateMessage lastMessage;

    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;

    @Column(name = "unread_count")
    private int unreadCount = 0;

    // 更新最后一条消息
    public void updateLastMessage(PrivateMessage message) {
        this.lastMessage = message;
        this.lastMessageTime = message.getCreatedAt();
    }

    // 增加未读数
    public void incrementUnreadCount() {
        this.unreadCount++;
    }

    // 重置未读数
    public void resetUnreadCount() {
        this.unreadCount = 0;
    }
} 