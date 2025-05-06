package com.jinshuxqm.community.config;

import com.jinshuxqm.community.dto.CommentDTO;
import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.repository.PostRepository;
import com.jinshuxqm.community.repository.UserRepository;
import com.jinshuxqm.community.service.CommentService;
import com.jinshuxqm.community.service.PostService;
import com.jinshuxqm.community.service.UserService;
import com.jinshuxqm.community.service.UserFollowService;
import com.jinshuxqm.community.agent.model.AgentConfig;
import com.jinshuxqm.community.agent.service.AgentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 定时任务：模拟多个Agent账号互相交互
 * 每5分钟随机选择一个Agent发布一篇帖子
 * 所有Agent会相互评论、点赞、关注和收藏，创造社区氛围
 */
@Component
@EnableScheduling
public class ScheduledTasks {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    
    private final Random random = new Random();
    private final Map<String, User> agentUsers = new HashMap<>();
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserFollowService userFollowService;
    
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
            logger.info("开始初始化Agent账号...");
            
            // 获取所有Agent配置
            List<AgentConfig> agentConfigs = agentManager.getAllAgentConfigs();
            
            // 为每个Agent创建账号
            for (AgentConfig config : agentConfigs) {
                // 检查Agent账号是否存在
                Optional<User> existingAgent = userRepository.findByUsername(config.getUsername());
                if (existingAgent.isPresent()) {
                    logger.info("Agent账号 {} 已存在，跳过创建步骤", config.getUsername());
                    agentUsers.put(config.getUsername(), existingAgent.get());
                } else {
                    // 创建Agent账号
                    User agent = new User();
                    agent.setUsername(config.getUsername());
                    agent.setPassword(passwordEncoder.encode(config.getPassword()));
                    agent.setEmail(config.getEmail());
                    agent.setNickname(config.getNickname());
                    agent.setBio(config.getBio());
                    agent.setCreatedAt(LocalDateTime.now());
                    agent.setUpdatedAt(LocalDateTime.now());
                    
                    User savedAgent = userRepository.save(agent);
                    agentUsers.put(config.getUsername(), savedAgent);
                    logger.info("Agent账号 {} 创建成功", config.getUsername());
                }
            }
            
            logger.info("所有Agent初始化完成，准备开始自动交互");
            
        } catch (Exception e) {
            logger.error("初始化Agent时出错", e);
        }
    }
    
    /**
     * 每5分钟随机选择一个Agent发布帖子
     */
    @Scheduled(fixedRate = 30000) // 每5分钟执行一次
    public void autoPost() {
        List<AgentConfig> agentConfigs = agentManager.getAllAgentConfigs();
        
        // 筛选当前活跃的Agent
        List<AgentConfig> activeAgents = agentConfigs.stream()
            .filter(AgentConfig::isActiveNow)
            .collect(Collectors.toList());
        
        if (activeAgents.isEmpty()) {
            logger.info("当前没有活跃的Agent，跳过发帖");
            return;
        }
        
        // 随机选择一个活跃的Agent
        AgentConfig selectedAgent = activeAgents.get(random.nextInt(activeAgents.size()));
        
        // 根据概率决定是否发帖
        if (random.nextDouble() <= selectedAgent.getPostProbability()) {
            createAgentPost(selectedAgent);
            logger.info("Agent {} 自动发帖已执行", selectedAgent.getUsername());
        } else {
            logger.info("Agent {} 决定不发帖", selectedAgent.getUsername());
        }
    }
    
    /**
     * 每1分钟执行一次，随机选择Agent进行交互行为（点赞、评论、关注、收藏）
     */
    @Scheduled(fixedRate = 60000) // 每1分钟执行一次
    public void randomInteraction() {
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
                User agentUser = agentUsers.get(selectedAgent.getUsername());
                
                if (agentUser == null) {
                    logger.warn("Agent {} 用户对象未找到，跳过互动", selectedAgent.getUsername());
                    continue;
                }
                
                // 创建Authentication对象
                Authentication agentAuth = createAgentAuthentication(agentUser);
                
                // 获取最近的10篇帖子
                Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
                List<Post> recentPosts = postRepository.findAll(pageable).getContent();
                
                if (recentPosts.isEmpty()) {
                    logger.info("没有找到最近的帖子，跳过互动");
                    continue;
                }
                
                // 随机选择1-3篇帖子进行互动
                int interactionCount = Math.min(recentPosts.size(), random.nextInt(3) + 1);
                for (int i = 0; i < interactionCount; i++) {
                    Post post = recentPosts.get(random.nextInt(recentPosts.size()));
                    
                    // 确保不是自己的帖子
                    if (!post.getAuthor().getUsername().equals(selectedAgent.getUsername())) {
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
                }
                
                // 使一些agent相互关注，增加网络效应
                if (random.nextDouble() < 0.3) { // 30%概率发生相互关注
                    try {
                        // 随机选择另一个agent相互关注
                        if (activeAgents.size() > 1) {
                            // 除了自己以外随机选择一个agent
                            List<AgentConfig> otherAgents = activeAgents.stream()
                                .filter(agent -> !agent.getUsername().equals(selectedAgent.getUsername()))
                                .collect(Collectors.toList());
                                
                            if (!otherAgents.isEmpty()) {
                                AgentConfig otherAgent = otherAgents.get(random.nextInt(otherAgents.size()));
                                User otherAgentUser = agentUsers.get(otherAgent.getUsername());
                                
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
                
                logger.info("Agent {} 完成了互动任务", selectedAgent.getUsername());
            }
        } catch (Exception e) {
            logger.error("执行Agent互动任务时出错", e);
        }
    }
    
    /**
     * 创建Agent的Authentication对象
     */
    private Authentication createAgentAuthentication(User agent) {
        return new UsernamePasswordAuthenticationToken(
            agent.getUsername(),
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
    
    /**
     * 创建Agent的帖子
     */
    private void createAgentPost(AgentConfig agentConfig) {
        try {
            User agent = agentUsers.get(agentConfig.getUsername());
            if (agent == null) {
                logger.warn("Agent账号 {} 不存在，无法创建帖子", agentConfig.getUsername());
                return;
            }
            
            // 随机选择标题和内容
            String title = agentConfig.getPostTitles().get(random.nextInt(agentConfig.getPostTitles().size()));
            String content = agentConfig.getPostContents().get(random.nextInt(agentConfig.getPostContents().size()));
            
            // 创建帖子
            try {
                com.jinshuxqm.community.model.dto.PostRequest postRequest = new com.jinshuxqm.community.model.dto.PostRequest();
                postRequest.setTitle(title);
                postRequest.setContent(content);
                postRequest.setTab("推荐");
                
                com.jinshuxqm.community.model.dto.PostResponse response = postService.createPost(postRequest, agent.getUsername());
                
                if (response != null && response.getId() != null) {
                    logger.info("Agent {} 成功发布新帖子: ID={}, 标题={}", agent.getUsername(), response.getId(), response.getTitle());
                } else {
                    logger.error("Agent创建帖子后返回的响应为null或无ID");
                }
            } catch (Exception e) {
                logger.error("使用PostService创建帖子时出错: {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            logger.error("创建Agent帖子时出错: {}", e.getMessage(), e);
        }
    }
} 