package com.jinshuxqm.community.config;

import com.jinshuxqm.community.agent.model.AgentConfig;
import com.jinshuxqm.community.agent.service.AgentManager;
import com.jinshuxqm.community.agent.service.AgentInitializationService;
import com.jinshuxqm.community.agent.service.AgentPostService;
import com.jinshuxqm.community.agent.service.AgentInteractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 定时任务调度器
 * 负责调度各种Agent任务，业务逻辑已模块化到专门的服务中
 */
@Component
public class ScheduledTasks {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    
    private final Random random = new Random();
    
    @Autowired
    private AgentManager agentManager;
    
    @Autowired
    private AgentInitializationService agentInitializationService;
    
    @Autowired
    private AgentPostService agentPostService;
    
    @Autowired
    private AgentInteractionService agentInteractionService;
    
    /**
     * 每10秒随机选择一个Agent发布帖子，发布后立即触发其他Agent评论
     */
    @Scheduled(fixedRate = 10000) // 每10秒执行一次（提高发帖频率）
    public void autoPost() {
        // 添加详细调试日志
        logger.info("🔄 === autoPost定时任务执行 - 当前时间: {} ===", LocalTime.now());
        
        List<AgentConfig> agentConfigs = agentManager.getAllAgentConfigs();
        logger.info("📊 获取到 {} 个Agent配置", agentConfigs.size());
        
        // 筛选当前活跃的Agent
        List<AgentConfig> activeAgents = agentConfigs.stream()
            .filter(AgentConfig::isActiveNow)
            .collect(Collectors.toList());
        
        logger.info("✨ 筛选后有 {} 个活跃Agent", activeAgents.size());
        if (!activeAgents.isEmpty()) {
            logger.info("🎯 活跃Agent列表: {}", 
                activeAgents.stream().map(AgentConfig::getUsername).collect(Collectors.toList()));
        }
        
        if (activeAgents.isEmpty()) {
            logger.warn("⚠️ 当前没有活跃的Agent，跳过发帖");
            return;
        }
        
        // 随机选择一个活跃的Agent
        AgentConfig selectedAgent = activeAgents.get(random.nextInt(activeAgents.size()));
        double randomValue = random.nextDouble();
        logger.info("🎲 选中Agent: {}, 发帖概率: {}, 随机值: {}", 
            selectedAgent.getUsername(), selectedAgent.getPostProbability(), randomValue);
        
        // 根据概率决定是否发帖
        if (randomValue <= selectedAgent.getPostProbability()) {
            logger.info("🚀 开始执行Agent {} 发帖（将立即触发其他Agent评论）", selectedAgent.getUsername());
            agentPostService.createAgentPost(selectedAgent);
            logger.info("✅ Agent {} 自动发帖流程已完成", selectedAgent.getUsername());
        } else {
            logger.info("⏭️ Agent {} 根据概率决定不发帖 (随机值: {} > 概率: {})", 
                selectedAgent.getUsername(), randomValue, selectedAgent.getPostProbability());
        }
    }
    
    /**
     * 每1分钟执行一次，随机选择Agent进行交互行为（点赞、评论、关注、收藏）
     */
    @Scheduled(fixedRate = 60000) // 每1分钟执行一次
    public void randomInteraction() {
        logger.debug("🔄 开始执行随机互动任务");
        agentInteractionService.performRandomInteraction();
    }
    
    /**
     * 每5秒让Agent对24小时内发布的最新帖子进行评论
     * 与立即评论机制配合，避免重复评论
     */
    @Scheduled(fixedRate = 5000) // 每5秒执行一次
    public void agentCommentOnExistingPosts() {
        logger.debug("🔄 开始Agent对现有帖子评论任务");
        agentInteractionService.commentOnExistingPosts();
    }

    /**
     * 小明专属发帖任务 - 每30秒发一次帖子
     */
    @Scheduled(fixedRate = 30000) // 每30秒执行一次
    public void xiaoMingAutoPost() {
        try {
            logger.info("🎯 === 小明专属发帖任务执行 - 当前时间: {} ===", LocalTime.now());
            logger.info("🔄 定时任务线程: {}", Thread.currentThread().getName());
            
            // 获取小明的Agent配置
            AgentConfig xiaoMingConfig = agentManager.getAgentConfigByUsername("xiaoming");
            
            if (xiaoMingConfig == null) {
                logger.warn("⚠️ 小明Agent配置未找到，跳过发帖");
                return;
            }
            
            if (!xiaoMingConfig.isActiveNow()) {
                logger.info("⏰ 小明当前不活跃，跳过发帖");
                return;
            }
            
            logger.info("🚀 小明开始发帖...");
            agentPostService.createAgentPost(xiaoMingConfig);
            logger.info("✅ 小明发帖完成！");
            
        } catch (Exception e) {
            logger.error("❌ 小明发帖任务执行出错: {}", e.getMessage(), e);
        }
    }
} 