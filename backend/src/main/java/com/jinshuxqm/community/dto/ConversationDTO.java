package com.jinshuxqm.community.dto;

import com.jinshuxqm.community.model.MessageConversation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
    private Long id;
    private Long partnerId;
    private String partnerUsername;
    private String partnerAvatar;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private int unreadCount;

    // 从实体对象构建DTO（针对当前用户的视角）
    public static ConversationDTO fromEntity(MessageConversation conversation, Long currentUserId) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        
        // 确定会话对象（非当前用户的一方）
        boolean isUser1 = conversation.getUser1().getId().equals(currentUserId);
        dto.setPartnerId(isUser1 ? conversation.getUser2().getId() : conversation.getUser1().getId());
        dto.setPartnerUsername(isUser1 ? conversation.getUser2().getUsername() : conversation.getUser1().getUsername());
        dto.setPartnerAvatar(isUser1 ? conversation.getUser2().getAvatar() : conversation.getUser1().getAvatar());
        
        // 设置最后一条消息信息
        if (conversation.getLastMessage() != null) {
            dto.setLastMessageContent(conversation.getLastMessage().getContent());
            dto.setLastMessageTime(conversation.getLastMessageTime());
        }
        
        dto.setUnreadCount(conversation.getUnreadCount());
        return dto;
    }
} 