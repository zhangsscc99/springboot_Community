package com.jinshuxqm.community.service;

import com.jinshuxqm.community.dto.PrivateMessageDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 服务器发送事件(SSE)服务接口
 * 用于管理SSE连接和发送事件
 */
public interface SseService {
    
    /**
     * 为用户创建新的SSE连接
     * @param userId 用户ID
     * @return SSE发射器
     */
    SseEmitter createConnection(Long userId);
    
    /**
     * 发送私信事件通知
     * @param userId 接收通知的用户ID
     * @param message 私信内容
     */
    void sendPrivateMessageEvent(Long userId, PrivateMessageDTO message);
    
    /**
     * 发送未读消息计数更新通知
     * @param userId 接收通知的用户ID
     * @param unreadCount 未读消息数
     */
    void sendUnreadCountEvent(Long userId, int unreadCount);
    
    /**
     * 关闭指定用户的连接
     * @param userId 用户ID
     */
    void closeConnection(Long userId);
} 