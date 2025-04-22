package com.jinshuxqm.community.service.impl;

import com.jinshuxqm.community.dto.PrivateMessageDTO;
import com.jinshuxqm.community.service.SseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseServiceImpl implements SseService {
    private static final Logger logger = LoggerFactory.getLogger(SseServiceImpl.class);
    
    // 存储用户ID和对应的SSE连接
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    // SSE连接的超时时间（毫秒）
    private static final long SSE_TIMEOUT = 3600000L; // 1小时
    
    @Override
    public SseEmitter createConnection(Long userId) {
        // 如果已存在连接，先关闭
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            emitter.complete();
        }
        
        // 创建新的SSE连接
        emitter = new SseEmitter(SSE_TIMEOUT);
        
        // 设置连接完成或超时的回调
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> {
            logger.error("Error in SSE connection for user {}: {}", userId, e.getMessage());
            emitters.remove(userId);
        });
        
        // 存储连接
        emitters.put(userId, emitter);
        
        // 发送初始连接事件
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("Connected successfully"));
        } catch (IOException e) {
            logger.error("Error sending initial SSE event to user {}: {}", userId, e.getMessage());
            emitter.completeWithError(e);
            return new SseEmitter();
        }
        
        return emitter;
    }
    
    @Override
    public void sendPrivateMessageEvent(Long userId, PrivateMessageDTO message) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("privateMessage")
                        .data(message));
                
                // 同时发送未读计数更新
                sendUnreadCountEvent(userId, -1); // -1表示需要客户端自行查询
            } catch (IOException e) {
                logger.error("Error sending private message event to user {}: {}", userId, e.getMessage());
                emitters.remove(userId);
                emitter.completeWithError(e);
            }
        }
    }
    
    @Override
    public void sendUnreadCountEvent(Long userId, int unreadCount) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("unreadCount")
                        .data(unreadCount));
            } catch (IOException e) {
                logger.error("Error sending unread count event to user {}: {}", userId, e.getMessage());
                emitters.remove(userId);
                emitter.completeWithError(e);
            }
        }
    }
    
    @Override
    public void closeConnection(Long userId) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            emitter.complete();
            emitters.remove(userId);
        }
    }
    
    // 获取当前活跃连接数
    public int getActiveConnectionCount() {
        return emitters.size();
    }
} 