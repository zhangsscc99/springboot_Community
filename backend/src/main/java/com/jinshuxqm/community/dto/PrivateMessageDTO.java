package com.jinshuxqm.community.dto;

import com.jinshuxqm.community.model.PrivateMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessageDTO {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderAvatar;
    private Long receiverId;
    private String receiverUsername;
    private String receiverAvatar;
    private String content;
    private boolean read;
    private LocalDateTime createdAt;

    // 从实体对象构建DTO
    public static PrivateMessageDTO fromEntity(PrivateMessage message) {
        PrivateMessageDTO dto = new PrivateMessageDTO();
        dto.setId(message.getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setSenderAvatar(message.getSender().getAvatar());
        dto.setReceiverId(message.getReceiver().getId());
        dto.setReceiverUsername(message.getReceiver().getUsername());
        dto.setReceiverAvatar(message.getReceiver().getAvatar());
        dto.setContent(message.getContent());
        dto.setRead(message.isRead());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
} 