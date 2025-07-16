package com.jinshuxqm.community.agent.service;

import com.jinshuxqm.community.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Agent认证服务
 * 负责Agent的认证相关逻辑
 */
@Service
public class AgentAuthService {
    
    /**
     * 创建Agent的Authentication对象
     */
    public Authentication createAgentAuthentication(User agent) {
        return new UsernamePasswordAuthenticationToken(
            agent.getUsername(),
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
} 