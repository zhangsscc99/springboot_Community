package com.jinshuxqm.community.agent.service;

import com.jinshuxqm.community.agent.config.AgentConfigProvider;
import com.jinshuxqm.community.agent.model.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent管理器
 * 负责收集、管理和提供所有Agent配置
 */
@Service
public class AgentManager {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentManager.class);
    
    private final Map<String, AgentConfig> agentConfigMap = new ConcurrentHashMap<>();
    private final List<AgentConfig> agentConfigs = new ArrayList<>();
    
    @Autowired
    private List<AgentConfigProvider> agentConfigProviders;
    
    /**
     * 初始化所有Agent配置
     */
    @PostConstruct
    public void init() {
        logger.info("开始初始化Agent配置...");
        
        for (AgentConfigProvider provider : agentConfigProviders) {
            AgentConfig config = provider.getAgentConfig();
            agentConfigMap.put(config.getUsername(), config);
            agentConfigs.add(config);
            
            logger.info("加载Agent配置: {} ({}岁)", config.getNickname(), config.getAge());
        }
        
        logger.info("Agent配置初始化完成，共加载 {} 个Agent", agentConfigs.size());
    }
    
    /**
     * 获取所有Agent配置
     */
    public List<AgentConfig> getAllAgentConfigs() {
        return agentConfigs;
    }
    
    /**
     * 根据用户名获取Agent配置
     */
    public AgentConfig getAgentConfigByUsername(String username) {
        return agentConfigMap.get(username);
    }
    
    /**
     * 获取Agent配置数量
     */
    public int getAgentCount() {
        return agentConfigs.size();
    }
} 