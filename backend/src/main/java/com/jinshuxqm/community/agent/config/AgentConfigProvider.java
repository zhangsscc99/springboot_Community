package com.jinshuxqm.community.agent.config;

import com.jinshuxqm.community.agent.model.AgentConfig;

/**
 * Agent配置提供者接口
 * 每个具体的Agent配置类需要实现该接口
 */
public interface AgentConfigProvider {
    
    /**
     * 获取Agent配置对象
     * @return AgentConfig 配置对象
     */
    AgentConfig getAgentConfig();
    
    /**
     * 获取Agent名称
     * @return Agent名称
     */
    String getAgentName();
} 