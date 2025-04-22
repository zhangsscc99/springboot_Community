package com.jinshuxqm.community.service.impl;

import com.jinshuxqm.community.dto.ConversationDTO;
import com.jinshuxqm.community.dto.PrivateMessageDTO;
import com.jinshuxqm.community.exception.ResourceNotFoundException;
import com.jinshuxqm.community.model.MessageConversation;
import com.jinshuxqm.community.model.PrivateMessage;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.repository.MessageConversationRepository;
import com.jinshuxqm.community.repository.PrivateMessageRepository;
import com.jinshuxqm.community.repository.UserRepository;
import com.jinshuxqm.community.service.PrivateMessageService;
import com.jinshuxqm.community.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrivateMessageServiceImpl implements PrivateMessageService {

    private final UserRepository userRepository;
    private final PrivateMessageRepository privateMessageRepository;
    private final MessageConversationRepository conversationRepository;
    private final SseService sseService;

    @Override
    @Transactional
    public PrivateMessageDTO sendMessage(Long senderId, Long receiverId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("发送者不存在"));
        
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("接收者不存在"));
        
        // 创建并保存消息
        PrivateMessage message = new PrivateMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        
        PrivateMessage savedMessage = privateMessageRepository.save(message);
        
        // 更新或创建会话
        updateConversation(savedMessage);
        
        // 通过SSE发送实时通知
        sseService.sendPrivateMessageEvent(receiverId, PrivateMessageDTO.fromEntity(savedMessage));
        
        return PrivateMessageDTO.fromEntity(savedMessage);
    }

    @Override
    public Page<PrivateMessageDTO> getMessagesBetweenUsers(Long userId, Long partnerId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        User partner = userRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("聊天对象不存在"));
        
        // 获取消息并转换为DTO
        Page<PrivateMessage> messages = privateMessageRepository.findMessagesBetweenUsers(user, partner, pageable);
        
        List<PrivateMessageDTO> dtos = messages.getContent().stream()
                .map(PrivateMessageDTO::fromEntity)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageable, messages.getTotalElements());
    }

    @Override
    public Page<ConversationDTO> getUserConversations(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        // 获取用户的所有会话
        Page<MessageConversation> conversations = conversationRepository.findConversationsForUser(user, pageable);
        
        List<ConversationDTO> dtos = conversations.getContent().stream()
                .map(conv -> ConversationDTO.fromEntity(conv, userId))
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageable, conversations.getTotalElements());
    }

    @Override
    @Transactional
    public void markMessagesAsRead(Long userId, Long partnerId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        User partner = userRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("聊天对象不存在"));
        
        // 查找partner发给user的所有未读消息
        List<PrivateMessage> unreadMessages = privateMessageRepository.findBySenderAndReceiverAndReadFalse(partner, user);
        
        if (!unreadMessages.isEmpty()) {
            // 获取消息ID列表
            List<Long> messageIds = unreadMessages.stream()
                    .map(PrivateMessage::getId)
                    .collect(Collectors.toList());
            
            // 标记为已读
            privateMessageRepository.markMessagesAsRead(messageIds);
            
            // 更新会话的未读计数
            Optional<MessageConversation> conversation = conversationRepository.findConversationBetweenUsers(user, partner);
            if (conversation.isPresent()) {
                MessageConversation conv = conversation.get();
                conv.resetUnreadCount();
                conversationRepository.save(conv);
            }
        }
    }

    @Override
    public int getUnreadMessageCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        return privateMessageRepository.findByReceiverAndReadFalse(user).size();
    }

    @Override
    public ConversationDTO getConversationDetails(Long userId, Long partnerId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        User partner = userRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("聊天对象不存在"));
        
        Optional<MessageConversation> conversation = conversationRepository.findConversationBetweenUsers(user, partner);
        
        return conversation.map(conv -> ConversationDTO.fromEntity(conv, userId))
                .orElse(null);
    }
    
    // 辅助方法：更新或创建会话
    private void updateConversation(PrivateMessage message) {
        User sender = message.getSender();
        User receiver = message.getReceiver();
        
        // 确保user1的ID小于user2的ID，以便一致地找到会话
        final User user1;
        final User user2;
        
        if (sender.getId() > receiver.getId()) {
            user1 = receiver;
            user2 = sender;
        } else {
            user1 = sender;
            user2 = receiver;
        }
        
        // 查找或创建会话
        MessageConversation conversation = conversationRepository.findConversationBetweenUsers(user1, user2)
                .orElseGet(() -> {
                    MessageConversation newConv = new MessageConversation();
                    newConv.setUser1(user1);
                    newConv.setUser2(user2);
                    return newConv;
                });
        
        // 更新会话信息
        conversation.updateLastMessage(message);
        
        // 如果当前用户不是发送者，则增加未读计数
        if (!message.getReceiver().equals(user1)) {
            conversation.incrementUnreadCount();
        }
        
        conversationRepository.save(conversation);
    }
} 