package com.jinshuxqm.community.agent.service;

import com.jinshuxqm.community.agent.model.AgentConfig;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.model.Role;
import com.jinshuxqm.community.model.ERole;
import com.jinshuxqm.community.repository.UserRepository;
import com.jinshuxqm.community.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Agent初始化服务
 * 负责Agent账号的创建、更新和管理
 */
@Service
public class AgentInitializationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentInitializationService.class);
    
    private final Map<String, User> agentUsers = new HashMap<>();
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AgentManager agentManager;
    
    /**
     * 应用启动时检查并创建所有Agent账号
     */
    @PostConstruct
    @Transactional
    public void initAgents() {
        try {
            logger.info("🚀 开始初始化Agent账号...");
            
            // 获取所有Agent配置
            List<AgentConfig> agentConfigs = agentManager.getAllAgentConfigs();
            logger.info("📋 Agent配置列表:");
            for (AgentConfig config : agentConfigs) {
                logger.info("  - {} ({}): {}", config.getUsername(), config.getNickname(), config.getEmail());
            }
            
            // 为每个Agent创建账号
            for (AgentConfig config : agentConfigs) {
                initSingleAgent(config);
            }
            
            logger.info("✅ 所有Agent初始化完成，准备开始自动交互");
            logger.info("📊 当前已初始化的Agent用户数量: {}", agentUsers.size());
            logger.info("📋 已初始化的Agent用户列表:");
            for (Map.Entry<String, User> entry : agentUsers.entrySet()) {
                User user = entry.getValue();
                logger.info("  - {} (ID: {}, 昵称: {})", entry.getKey(), user.getId(), user.getNickname());
            }
            
        } catch (Exception e) {
            logger.error("❌ 初始化Agent时出错", e);
        }
    }
    
    /**
     * 初始化单个Agent
     */
    private void initSingleAgent(AgentConfig config) {
        // 检查Agent账号是否存在
        Optional<User> existingAgent = userRepository.findByUsername(config.getUsername());
        if (existingAgent.isPresent()) {
            logger.info("Agent账号 {} 已存在，跳过创建步骤", config.getUsername());
            User agent = existingAgent.get();
            agentUsers.put(config.getUsername(), agent);
            
            // 更新现有Agent的信息（如果缺失）
            updateExistingAgent(agent, config);
        } else {
            // 创建新的Agent账号
            createNewAgent(config);
        }
    }
    
    /**
     * 更新现有Agent的信息
     */
    private void updateExistingAgent(User agent, AgentConfig config) {
        boolean needUpdate = false;
        
        if (agent.getAvatar() == null || agent.getAvatar().isEmpty()) {
            agent.setAvatar(getDefaultAvatarForAgent(config.getUsername()));
            needUpdate = true;
            logger.info("为Agent {} 添加头像", config.getUsername());
        }
        
        if (agent.getNickname() == null || agent.getNickname().isEmpty()) {
            agent.setNickname(config.getNickname());
            needUpdate = true;
            logger.info("为Agent {} 添加昵称", config.getUsername());
        }
        
        if (agent.getBio() == null || agent.getBio().isEmpty()) {
            agent.setBio(config.getBio());
            needUpdate = true;
            logger.info("为Agent {} 添加简介", config.getUsername());
        }
        
        // 检查并添加角色（如果缺失）
        if (agent.getRoles() == null || agent.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            roleRepository.findByName(ERole.ROLE_USER).ifPresent(roles::add);
            agent.setRoles(roles);
            needUpdate = true;
            logger.info("为Agent {} 添加USER角色", config.getUsername());
        }
        
        if (needUpdate) {
            userRepository.save(agent);
            logger.info("✅ 更新Agent {} 的信息完成", config.getUsername());
        }
    }
    
    /**
     * 创建新的Agent账号
     */
    private void createNewAgent(AgentConfig config) {
        User agent = new User();
        agent.setUsername(config.getUsername());
        agent.setPassword(passwordEncoder.encode(config.getPassword()));
        agent.setEmail(config.getEmail());
        agent.setNickname(config.getNickname());
        agent.setBio(config.getBio());
        agent.setAvatar(getDefaultAvatarForAgent(config.getUsername()));
        agent.setCreatedAt(LocalDateTime.now());
        agent.setUpdatedAt(LocalDateTime.now());
        
        // 为Agent设置USER角色
        Set<Role> roles = new HashSet<>();
        roleRepository.findByName(ERole.ROLE_USER).ifPresent(roles::add);
        agent.setRoles(roles);
        
        User savedAgent = userRepository.save(agent);
        agentUsers.put(config.getUsername(), savedAgent);
        logger.info("✅ Agent账号 {} 创建成功，已设置USER角色", config.getUsername());
    }
    
    /**
     * 为不同的Agent获取默认头像
     */
    private String getDefaultAvatarForAgent(String agentUsername) {
        switch (agentUsername) {
            case "city_girl":
                return "https://i.pinimg.com/474x/81/8a/1b/818a1b89a91a4a90f5ff6dc70908c313.jpg";
            case "career_sister":
                return "https://i.pinimg.com/474x/2e/38/8e/2e388e5cb3a4de8f8b9a7f6f4a1b2c3d.jpg";
            case "teen_heart":
                return "https://i.pinimg.com/474x/5f/9a/2b/5f9a2b8c7d6e3f4a9b8c7d6e3f4a9b8c.jpg";
            case "family_man":
                return "https://i.pinimg.com/474x/4c/7d/9e/4c7d9e1f8a6b5c4d3e2f1a9b8c7d6e5f.jpg";
            case "lovelessboy":
                return "https://i.pinimg.com/474x/8b/4e/7f/8b4e7f2a9d6c5b4e3f2a1d9c8b7e6f5a.jpg";
            case "xiaoming":
                return "https://i.pinimg.com/474x/12/34/56/123456789abcdef0123456789abcdef0.jpg";
            default:
                return "https://via.placeholder.com/50x50/007bff/ffffff?text=AI";
        }
    }
    
    /**
     * 获取Agent用户对象
     */
    public User getAgentUser(String username) {
        User agent = agentUsers.get(username);
        if (agent == null) {
            logger.warn("Agent {} 用户对象未找到，尝试重新获取", username);
            // 尝试从数据库重新获取
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                agent = userOpt.get();
                agentUsers.put(username, agent);
                logger.info("✅ 成功从数据库重新获取Agent {} 用户对象", username);
            } else {
                logger.error("❌ 数据库中也找不到Agent {} 用户", username);
            }
        }
        return agent;
    }
    
    /**
     * 获取所有Agent用户
     */
    public Map<String, User> getAllAgentUsers() {
        return new HashMap<>(agentUsers);
    }
    
    /**
     * 重新初始化所有Agent
     */
    @Transactional
    public void reinitAllAgents() {
        logger.info("🔄 重新初始化所有Agent...");
        agentUsers.clear();
        initAgents();
    }
} 