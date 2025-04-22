package com.jinshuxqm.community.service;

import com.jinshuxqm.community.dto.ConversationDTO;
import com.jinshuxqm.community.dto.PrivateMessageDTO;
import com.jinshuxqm.community.model.PrivateMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PrivateMessageService {
    
    // 发送消息
    PrivateMessageDTO sendMessage(Long senderId, Long receiverId, String content);
    
    // 获取两个用户之间的消息记录
    Page<PrivateMessageDTO> getMessagesBetweenUsers(Long userId, Long partnerId, Pageable pageable);
    
    // 获取用户的所有会话列表
    Page<ConversationDTO> getUserConversations(Long userId, Pageable pageable);
    
    // 将消息标记为已读
    void markMessagesAsRead(Long userId, Long partnerId);
    
    // 获取用户未读消息数
    int getUnreadMessageCount(Long userId);
    
    // 获取特定会话的详情
    ConversationDTO getConversationDetails(Long userId, Long partnerId);
} 