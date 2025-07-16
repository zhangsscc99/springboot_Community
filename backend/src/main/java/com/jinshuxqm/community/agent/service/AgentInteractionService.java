package com.jinshuxqm.community.agent.service;

import com.jinshuxqm.community.agent.model.AgentConfig;
import com.jinshuxqm.community.dto.CommentDTO;
import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.repository.PostRepository;
import com.jinshuxqm.community.service.CommentService;
import com.jinshuxqm.community.service.PostService;
import com.jinshuxqm.community.service.UserFollowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Agent互动服务
 * 负责Agent之间的互动逻辑（点赞、评论、关注、收藏等）
 */
@Service
public class AgentInteractionService {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentInteractionService.class);
    
    private final Random random = new Random();
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private UserFollowService userFollowService;
    
    @Autowired
    private AgentManager agentManager;
    
    @Autowired
    private AgentInitializationService agentInitializationService;
    
    @Autowired
    private AgentAuthService agentAuthService;
    
    /**
     * 执行随机互动
     */
    public void performRandomInteraction() {
        try {
            List<AgentConfig> agentConfigs = agentManager.getAllAgentConfigs();
            
            // 筛选当前活跃的Agent
            List<AgentConfig> activeAgents = agentConfigs.stream()
                .filter(AgentConfig::isActiveNow)
                .collect(Collectors.toList());
            
            if (activeAgents.isEmpty()) {
                logger.info("当前没有活跃的Agent，跳过互动");
                return;
            }
            
            // 随机选择1-3个活跃的Agent执行互动
            int agentCount = Math.min(activeAgents.size(), random.nextInt(3) + 1);
            for (int a = 0; a < agentCount; a++) {
                AgentConfig selectedAgent = activeAgents.get(random.nextInt(activeAgents.size()));
                performAgentInteraction(selectedAgent);
            }
        } catch (Exception e) {
            logger.error("执行Agent互动任务时出错", e);
        }
    }
    
    /**
     * 执行单个Agent的互动
     */
    private void performAgentInteraction(AgentConfig selectedAgent) {
        User agentUser = agentInitializationService.getAgentUser(selectedAgent.getUsername());
        
        if (agentUser == null) {
            logger.error("❌ Agent {} 用户对象未找到，跳过互动", selectedAgent.getUsername());
            return;
        }
        
        // 创建Authentication对象
        Authentication agentAuth = agentAuthService.createAgentAuthentication(agentUser);
        
        // 获取24小时内的最新10篇帖子
        List<Post> recentPosts = getRecentPosts();
        
        if (recentPosts.isEmpty()) {
            logger.info("没有找到24小时内的帖子，跳过互动");
            return;
        }
        
        logger.debug("Agent {} 找到 {} 篇24小时内的帖子进行互动", selectedAgent.getUsername(), recentPosts.size());
        
        // 随机选择1-3篇帖子进行互动
        int interactionCount = Math.min(recentPosts.size(), random.nextInt(3) + 1);
        for (int i = 0; i < interactionCount; i++) {
            Post post = recentPosts.get(random.nextInt(recentPosts.size()));
            
            // 确保不是自己的帖子
            if (!post.getAuthor().getUsername().equals(selectedAgent.getUsername())) {
                performPostInteraction(selectedAgent, agentUser, agentAuth, post);
            }
        }
        
        // Agent相互关注
        performAgentFollowing(selectedAgent, agentUser);
        
        logger.info("Agent {} 完成了互动任务", selectedAgent.getUsername());
    }
    
    /**
     * 对帖子执行互动
     */
    private void performPostInteraction(AgentConfig selectedAgent, User agentUser, Authentication agentAuth, Post post) {
        // 点赞操作
        if (random.nextDouble() <= selectedAgent.getLikeProbability()) {
            try {
                // 检查是否已经点赞过
                boolean alreadyLiked = false;
                if (post.getLikes() != null) {
                    alreadyLiked = post.getLikes().stream()
                            .anyMatch(like -> like.getUser().getUsername().equals(selectedAgent.getUsername()));
                }
                
                if (!alreadyLiked) {
                    postService.likePost(post.getId(), agentUser.getUsername());
                    logger.info("Agent {} 点赞了帖子 {}", selectedAgent.getUsername(), post.getId());
                }
            } catch (Exception e) {
                logger.error("点赞帖子时出错: {}", e.getMessage());
            }
        }
        
        // 评论操作
        if (random.nextDouble() <= selectedAgent.getCommentProbability()) {
            try {
                // 随机选择一条评论
                String comment = selectedAgent.getComments().get(random.nextInt(selectedAgent.getComments().size()));
                
                CommentDTO commentDTO = new CommentDTO();
                commentDTO.setContent(comment);
                commentDTO.setPostId(post.getId());
                
                commentService.createComment(post.getId(), commentDTO, agentAuth);
                logger.info("Agent {} 评论了帖子 {}: {}", selectedAgent.getUsername(), post.getId(), comment);
            } catch (Exception e) {
                logger.error("评论帖子时出错: {}", e.getMessage());
            }
        }
        
        // 收藏操作
        if (random.nextDouble() <= selectedAgent.getFavoriteProbability()) {
            try {
                postService.favoritePost(post.getId(), agentUser.getUsername());
                logger.info("Agent {} 收藏了帖子 {}", selectedAgent.getUsername(), post.getId());
            } catch (Exception e) {
                logger.error("收藏帖子时出错: {}", e.getMessage());
            }
        }
        
        // 关注作者操作
        if (random.nextDouble() <= selectedAgent.getFollowProbability()) {
            try {
                Long followerId = agentUser.getId();
                Long followeeId = post.getAuthor().getId();
                
                // 检查是否已经关注
                boolean isAlreadyFollowing = userFollowService.isFollowing(followerId, followeeId);
                
                if (!isAlreadyFollowing) {
                    userFollowService.followUser(followerId, followeeId);
                    logger.info("Agent {} 关注了用户 {}", selectedAgent.getUsername(), post.getAuthor().getUsername());
                }
            } catch (Exception e) {
                logger.error("关注用户时出错: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Agent相互关注
     */
    private void performAgentFollowing(AgentConfig selectedAgent, User agentUser) {
        // 使一些agent相互关注，增加网络效应
        if (random.nextDouble() < 0.3) { // 30%概率发生相互关注
            try {
                List<AgentConfig> allAgents = agentManager.getAllAgentConfigs();
                List<AgentConfig> activeAgents = allAgents.stream()
                    .filter(AgentConfig::isActiveNow)
                    .collect(Collectors.toList());
                
                // 除了自己以外随机选择一个agent
                if (activeAgents.size() > 1) {
                    List<AgentConfig> otherAgents = activeAgents.stream()
                        .filter(agent -> !agent.getUsername().equals(selectedAgent.getUsername()))
                        .collect(Collectors.toList());
                        
                    if (!otherAgents.isEmpty()) {
                        AgentConfig otherAgent = otherAgents.get(random.nextInt(otherAgents.size()));
                        User otherAgentUser = agentInitializationService.getAgentUser(otherAgent.getUsername());
                        
                        if (otherAgentUser != null) {
                            Long followerId = agentUser.getId();
                            Long followeeId = otherAgentUser.getId();
                            
                            // 检查是否已经关注
                            boolean isAlreadyFollowing = userFollowService.isFollowing(followerId, followeeId);
                            
                            if (!isAlreadyFollowing) {
                                userFollowService.followUser(followerId, followeeId);
                                logger.info("Agent {} 关注了其他Agent {}", selectedAgent.getUsername(), otherAgent.getUsername());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Agent互相关注时出错: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Agent对24小时内发布的最新帖子进行评论
     * 与立即评论机制配合，避免重复评论
     */
    @Transactional // 添加事务注解以避免LazyInitializationException
    public void commentOnExistingPosts() {
        try {
            logger.info("=== 开始Agent对24小时内最新帖子评论任务 ===");
            
            List<AgentConfig> agentConfigs = agentManager.getAllAgentConfigs();
            
            // 筛选当前活跃的Agent
            List<AgentConfig> activeAgents = agentConfigs.stream()
                .filter(AgentConfig::isActiveNow)
                .collect(Collectors.toList());
            
            if (activeAgents.isEmpty()) {
                logger.info("当前没有活跃的Agent，跳过评论任务");
                return;
            }
            
            // 获取24小时内发布的最新帖子（所有栏目）
            LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5); // 5分钟前
            
            // 获取24小时内的最新帖子，按创建时间倒序，限制30个
            Pageable pageable = PageRequest.of(0, 30, Sort.by(Sort.Direction.DESC, "createdAt"));
            List<Post> recentPosts = postRepository.findAll(pageable).getContent()
                .stream()
                .filter(post -> post.getCreatedAt().isAfter(twentyFourHoursAgo)) // 24小时内
                .filter(post -> post.getCreatedAt().isBefore(fiveMinutesAgo)) // 跳过5分钟内的新帖子，避免与立即评论机制冲突
                .collect(Collectors.toList());
            
            logger.info("找到24小时内发布的最新帖子数量（排除5分钟内新帖子）: {}", recentPosts.size());
            if (!recentPosts.isEmpty()) {
                Post latestPost = recentPosts.get(0);
                Post oldestPost = recentPosts.get(recentPosts.size() - 1);
                logger.info("帖子ID范围: {} (最新) 到 {} (最旧)", latestPost.getId(), oldestPost.getId());
            }
            
            if (recentPosts.isEmpty()) {
                logger.info("24小时内没有符合条件的帖子，跳过评论任务");
                return;
            }
            
            // 找到评论数不足的帖子进行补充评论（优先处理最新的帖子）
            int commentedPosts = 0;
            for (Post post : recentPosts) {
                if (processPostForComments(post, activeAgents)) {
                    commentedPosts++;
                    // 每次只处理一篇帖子，避免过于密集
                    if (commentedPosts >= 1) {
                        break;
                    }
                }
            }
            
            if (commentedPosts == 0) {
                logger.info("✅ 所有帖子都已有足够评论，无需补充");
            }
            
        } catch (Exception e) {
            logger.error("Agent评论当天帖子任务执行出错: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理单个帖子的评论需求
     */
    private boolean processPostForComments(Post post, List<AgentConfig> activeAgents) {
        // 检查当前评论数量
        List<CommentDTO> existingComments = commentService.getCommentsByPostId(post.getId());
        int currentCommentCount = existingComments.size();
        
        // 计算应该有多少个Agent评论（排除作者自己）
        int expectedCommentCount = (int) activeAgents.stream()
            .filter(agent -> !agent.getUsername().equals(post.getAuthor().getUsername()))
            .count();
        
        // 如果评论数已经足够，跳过这篇帖子
        if (currentCommentCount >= expectedCommentCount) {
            logger.debug("帖子 {} 已有足够评论 ({}/{}), 跳过", post.getId(), currentCommentCount, expectedCommentCount);
            return false;
        }
        
        logger.info("📝 准备为帖子 {} (标题: {}) 补充评论 (当前:{}/期望:{})", 
            post.getId(), post.getTitle(), currentCommentCount, expectedCommentCount);
        
        // 找出还没有评论的Agent
        List<String> existingCommenterUsernames = existingComments.stream()
            .map(comment -> comment.getAuthor().getUsername())
            .collect(Collectors.toList());
        
        List<AgentConfig> uncommentedAgents = activeAgents.stream()
            .filter(agent -> !agent.getUsername().equals(post.getAuthor().getUsername())) // 排除作者
            .filter(agent -> !existingCommenterUsernames.contains(agent.getUsername())) // 排除已评论的
            .collect(Collectors.toList());
        
        // 让未评论的Agent进行评论
        for (int i = 0; i < uncommentedAgents.size(); i++) {
            AgentConfig selectedAgent = uncommentedAgents.get(i);
            User agentUser = agentInitializationService.getAgentUser(selectedAgent.getUsername());
            
            if (agentUser == null) {
                logger.error("❌ Agent {} 用户对象未找到，跳过评论", selectedAgent.getUsername());
                continue;
            }
            
            // 为了避免过于密集，添加适当延迟
            final int delay = i * 800; // 每个Agent延迟800ms
            final AgentConfig finalAgent = selectedAgent;
            final User finalUser = agentUser;
            final Post finalPost = post;
            
            // 异步延迟评论
            new Thread(() -> {
                try {
                    if (delay > 0) {
                        Thread.sleep(delay);
                    }
                    
                    // 在新线程中创建评论，确保事务上下文
                    createCommentInNewTransaction(finalAgent, finalUser, finalPost);
                        
                } catch (Exception e) {
                    logger.error("Agent {} 补充评论帖子 {} 时出错: {}", 
                        finalAgent.getUsername(), finalPost.getId(), e.getMessage());
                }
            }).start();
        }
        
        return true;
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
    
    /**
     * 获取24小时内的最新帖子
     */
    private List<Post> getRecentPosts() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findAll(pageable).getContent()
            .stream()
            .filter(post -> post.getCreatedAt().isAfter(twentyFourHoursAgo)) // 只获取24小时内的帖子
            .limit(10) // 限制为10篇
            .collect(Collectors.toList());
    }
} 