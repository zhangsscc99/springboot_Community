package com.jinshuxqm.community.agent.service;

import com.jinshuxqm.community.agent.model.AgentConfig;
import com.jinshuxqm.community.dto.CommentDTO;
import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.repository.PostRepository;
import com.jinshuxqm.community.service.CommentService;
import com.jinshuxqm.community.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Agent发帖服务
 * 负责Agent发帖和新帖子评论触发的逻辑
 */
@Service
public class AgentPostService {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentPostService.class);
    
    private final Random random = new Random();
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private AgentManager agentManager;
    
    @Autowired
    private AgentInitializationService agentInitializationService;
    
    @Autowired
    private AgentAuthService agentAuthService;
    
    /**
     * 创建Agent的帖子
     */
    public void createAgentPost(AgentConfig agentConfig) {
        try {
            User agent = agentInitializationService.getAgentUser(agentConfig.getUsername());
            if (agent == null) {
                logger.warn("Agent账号 {} 不存在，无法创建帖子", agentConfig.getUsername());
                return;
            }
            
            // 随机选择标题和内容
            String title = agentConfig.getPostTitles().get(random.nextInt(agentConfig.getPostTitles().size()));
            String content = agentConfig.getPostContents().get(random.nextInt(agentConfig.getPostContents().size()));
            
            logger.info("📝 Agent {} 正在发布新帖子: {}", agent.getUsername(), title);
            
            // 创建帖子
            try {
                com.jinshuxqm.community.model.dto.PostRequest postRequest = new com.jinshuxqm.community.model.dto.PostRequest();
                postRequest.setTitle(title);
                postRequest.setContent(content);
                postRequest.setTab("推荐"); // 改回推荐栏目
                
                com.jinshuxqm.community.model.dto.PostResponse response = postService.createPost(postRequest, agent.getUsername());
                
                if (response != null && response.getId() != null) {
                    logger.info("✅ Agent {} 成功发布新帖子: ID={}, 标题={}", 
                        agent.getUsername(), response.getId(), response.getTitle());
                    
                    // 🚀 关键优化：立即触发其他Agent评论，实现丝滑的互动体验
                    logger.info("🚀 立即触发其他Agent对新帖子 {} 进行评论...", response.getId());
                    triggerAgentCommentsOnNewPost(response.getId(), agentConfig.getUsername());
                    
                } else {
                    logger.error("❌ Agent {} 创建帖子失败：返回的响应为null或无ID", agent.getUsername());
                }
            } catch (Exception e) {
                logger.error("❌ Agent {} 使用PostService创建帖子时出错: {}", agent.getUsername(), e.getMessage(), e);
            }
        } catch (Exception e) {
            logger.error("❌ 创建Agent帖子时出错: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 触发其他Agent对新帖子进行评论 - 优化版本：让评论更快出现
     * @param postId 新发布的帖子ID
     * @param authorUsername 发帖Agent的用户名（排除自己）
     */
    @Transactional // 添加事务注解确保数据库操作的一致性
    public void triggerAgentCommentsOnNewPost(Long postId, String authorUsername) {
        try {
            logger.info("=== 开始为新帖子 {} 触发Agent评论，发帖者: {} ===", postId, authorUsername);
            
            // 获取所有Agent配置，排除发帖的Agent
            List<AgentConfig> allAgents = agentManager.getAllAgentConfigs();
            logger.info("总共有 {} 个Agent配置", allAgents.size());
            
            List<AgentConfig> otherAgents = allAgents.stream()
                .filter(agent -> !agent.getUsername().equals(authorUsername))
                .collect(Collectors.toList());
            
            logger.info("排除发帖者后，有 {} 个Agent可以评论", otherAgents.size());
            
            if (otherAgents.isEmpty()) {
                logger.warn("没有其他Agent可以评论帖子 {}", postId);
                return;
            }
            
            // 先获取帖子信息，确保帖子存在
            final Post finalPost = postRepository.findById(postId).orElse(null);
            if (finalPost == null) {
                logger.warn("帖子 {} 不存在，跳过评论", postId);
                return;
            }
            
            // 让所有其他Agent都评论新帖子 - 使用更短的延迟让评论更快出现
            for (int i = 0; i < otherAgents.size(); i++) {
                AgentConfig commenterAgent = otherAgents.get(i);
                User commenterUser = agentInitializationService.getAgentUser(commenterAgent.getUsername());
                
                if (commenterUser == null) {
                    logger.warn("评论Agent {} 用户对象未找到，跳过评论", commenterAgent.getUsername());
                    continue;
                }
                
                // 大幅减少延迟时间：第一个Agent立即评论，其他Agent间隔100ms
                final int delay = i * 100; // 100毫秒间隔，让评论几乎立即出现
                final AgentConfig finalAgent = commenterAgent;
                final User finalUser = commenterUser;
                
                // 使用异步延迟执行评论
                new Thread(() -> {
                    try {
                        if (delay > 0) {
                            Thread.sleep(delay);
                        }
                        
                        // 在新线程中创建评论，确保事务上下文
                        createCommentInNewTransaction(finalAgent, finalUser, finalPost);
                            
                    } catch (Exception e) {
                        logger.error("❌ Agent {} 评论新帖子 {} 时出错: {}", 
                            finalAgent.getUsername(), finalPost.getId(), e.getMessage());
                    }
                }).start();
            }
            
            logger.info("🚀 完成为新帖子 {} 触发Agent评论，共 {} 个评论者（将在{}ms内快速完成）", 
                postId, otherAgents.size(), (otherAgents.size() - 1) * 100);
            
        } catch (Exception e) {
            logger.error("❌ 触发Agent评论新帖子时出错: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 在新线程中创建评论，确保事务上下文
     */
    private void createCommentInNewTransaction(AgentConfig agentConfig, User agentUser, Post post) {
        try {
            // 创建Authentication对象
            Authentication agentAuth = agentAuthService.createAgentAuthentication(agentUser);

            // 随机选择一条评论内容
            String commentContent = agentConfig.getComments().get(random.nextInt(agentConfig.getComments().size()));

            // 创建评论对象
            CommentDTO commentDTO = new CommentDTO();
            commentDTO.setContent(commentContent);
            commentDTO.setPostId(post.getId());

            // 发布评论
            CommentDTO createdComment = commentService.createComment(post.getId(), commentDTO, agentAuth);

            if (createdComment != null) {
                logger.info("✅ Agent {} 成功评论了帖子 {} (标题: {}): {}", 
                    agentConfig.getUsername(), post.getId(), post.getTitle(), commentContent);
            } else {
                logger.error("❌ Agent {} 评论创建失败", agentConfig.getUsername());
            }
        } catch (Exception e) {
            logger.error("❌ Agent {} 评论帖子 {} 时出错: {}", 
                agentConfig.getUsername(), post.getId(), e.getMessage());
        }
    }
} 