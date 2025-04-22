package com.jinshuxqm.community.controller;

import com.jinshuxqm.community.dto.ConversationDTO;
import com.jinshuxqm.community.dto.PrivateMessageDTO;
import com.jinshuxqm.community.dto.SendMessageRequest;
import com.jinshuxqm.community.security.services.UserDetailsImpl;
import com.jinshuxqm.community.service.PrivateMessageService;
import com.jinshuxqm.community.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class PrivateMessageController {

    private final PrivateMessageService messageService;
    private final SseService sseService;

    /**
     * 获取当前用户的所有会话
     */
    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ConversationDTO>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Long currentUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastMessageTime").descending());
        
        Page<ConversationDTO> conversations = messageService.getUserConversations(currentUserId, pageable);
        return ResponseEntity.ok(conversations);
    }

    /**
     * 获取特定会话的详情
     */
    @GetMapping("/conversations/{partnerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConversationDTO> getConversationDetails(@PathVariable Long partnerId) {
        Long currentUserId = getCurrentUserId();
        ConversationDTO conversation = messageService.getConversationDetails(currentUserId, partnerId);
        
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(conversation);
    }

    /**
     * 获取与特定用户的消息历史
     */
    @GetMapping("/history/{partnerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PrivateMessageDTO>> getMessageHistory(
            @PathVariable Long partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Long currentUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        // 标记消息为已读
        messageService.markMessagesAsRead(currentUserId, partnerId);
        
        Page<PrivateMessageDTO> messages = messageService.getMessagesBetweenUsers(currentUserId, partnerId, pageable);
        return ResponseEntity.ok(messages);
    }

    /**
     * 发送私信
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PrivateMessageDTO> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        Long currentUserId = getCurrentUserId();
        PrivateMessageDTO message = messageService.sendMessage(currentUserId, request.getReceiverId(), request.getContent());
        return new ResponseEntity<>(message, HttpStatus.CREATED);
    }

    /**
     * 标记与特定用户的消息为已读
     */
    @PutMapping("/read/{partnerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable Long partnerId) {
        Long currentUserId = getCurrentUserId();
        messageService.markMessagesAsRead(currentUserId, partnerId);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取未读消息数
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Integer> getUnreadCount() {
        Long currentUserId = getCurrentUserId();
        int count = messageService.getUnreadMessageCount(currentUserId);
        return ResponseEntity.ok(count);
    }

    /**
     * 创建SSE连接，用于接收实时消息
     */
    @GetMapping(value = "/sse-connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter connect() {
        Long currentUserId = getCurrentUserId();
        return sseService.createConnection(currentUserId);
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        return userDetails.getId();
    }
} 