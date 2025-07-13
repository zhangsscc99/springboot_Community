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
     * 每20秒随机选择一个Agent发布帖子
     */
    @Scheduled(fixedRate = 20000) // 每20秒执行一次
    public void autoPost() {
        // 添加详细调试日志
        logger.info("=== autoPost定时任务执行 - 当前时间: {} ===", LocalTime.now());
        
        List<AgentConfig> agentConfigs = agentManager.getAllAgentConfigs();
        logger.info("获取到 {} 个Agent配置", agentConfigs.size());
        
        // 筛选当前活跃的Agent
        List<AgentConfig> activeAgents = agentConfigs.stream()
            .filter(AgentConfig::isActiveNow)
            .collect(Collectors.toList());
        
        logger.info("筛选后有 {} 个活跃Agent", activeAgents.size());
        if (!activeAgents.isEmpty()) {
            logger.info("活跃Agent列表: {}", 
                activeAgents.stream().map(AgentConfig::getUsername).collect(Collectors.toList()));
        }
        
        if (activeAgents.isEmpty()) {
            logger.warn("当前没有活跃的Agent，跳过发帖");
            return;
        }
        
        // 随机选择一个活跃的Agent
        AgentConfig selectedAgent = activeAgents.get(random.nextInt(activeAgents.size()));
        double randomValue = random.nextDouble();
        logger.info("选中Agent: {}, 发帖概率: {}, 随机值: {}", 
            selectedAgent.getUsername(), selectedAgent.getPostProbability(), randomValue);
        
        // 根据概率决定是否发帖
        if (randomValue <= selectedAgent.getPostProbability()) {
            logger.info("开始执行Agent {} 发帖", selectedAgent.getUsername());
            createAgentPost(selectedAgent);
            logger.info("Agent {} 自动发帖已执行", selectedAgent.getUsername());
        } else {
            logger.info("Agent {} 根据概率决定不发帖 (随机值: {} > 概率: {})", 
                selectedAgent.getUsername(), randomValue, selectedAgent.getPostProbability());
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
     * 每5秒让Agent对关注栏目下的现有帖子进行评论（高频测试）
     */
    @Scheduled(fixedRate = 5000) // 每5秒执行一次
    public void agentCommentOnExistingPosts() {
        try {
            logger.info("=== 开始Agent对现有帖子评论任务 ===");
            
            List<AgentConfig> agentConfigs = agentManager.getAllAgentConfigs();
            
            // 筛选当前活跃的Agent
            List<AgentConfig> activeAgents = agentConfigs.stream()
                .filter(AgentConfig::isActiveNow)
                .collect(Collectors.toList());
            
            if (activeAgents.isEmpty()) {
                logger.info("当前没有活跃的Agent，跳过评论任务");
                return;
            }
            
            // 获取关注栏目下的帖子
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
            List<Post> posts = postRepository.findByTab("关注", pageable).getContent();
            
            logger.info("找到关注栏目下的帖子数量: {}", posts.size());
            
            if (posts.isEmpty()) {
                logger.info("关注栏目下没有帖子，跳过评论任务");
                return;
            }
            
            // 随机选择1-3个Agent进行评论（增加评论活跃度）
            int agentCount = Math.min(activeAgents.size(), random.nextInt(3) + 1);
            for (int i = 0; i < agentCount; i++) {
                AgentConfig selectedAgent = activeAgents.get(random.nextInt(activeAgents.size()));
                User agentUser = agentUsers.get(selectedAgent.getUsername());
                
                if (agentUser == null) {
                    logger.warn("Agent {} 用户对象未找到，跳过评论", selectedAgent.getUsername());
                    continue;
                }
                
                // 随机选择一个帖子进行评论
                Post selectedPost = posts.get(random.nextInt(posts.size()));
                
                // 检查是否是自己的帖子
                if (selectedPost.getAuthor().getUsername().equals(selectedAgent.getUsername())) {
                    logger.info("Agent {} 跳过评论自己的帖子 {}", selectedAgent.getUsername(), selectedPost.getId());
                    continue;
                }
                
                // 基于评论概率决定是否评论
                if (random.nextDouble() <= selectedAgent.getCommentProbability()) {
                    try {
                        // 随机选择一条评论内容
                        String commentContent = selectedAgent.getComments().get(
                            random.nextInt(selectedAgent.getComments().size()));
                        
                        // 创建评论对象
                        CommentDTO commentDTO = new CommentDTO();
                        commentDTO.setContent(commentContent);
                        commentDTO.setPostId(selectedPost.getId());
                        
                        // 创建Authentication对象
                        Authentication agentAuth = createAgentAuthentication(agentUser);
                        
                        // 发布评论
                        commentService.createComment(selectedPost.getId(), commentDTO, agentAuth);
                        logger.info("Agent {} 成功评论了关注栏目帖子 {} (标题: {}): {}", 
                            selectedAgent.getUsername(), selectedPost.getId(), selectedPost.getTitle(), commentContent);
                            
                    } catch (Exception e) {
                        logger.error("Agent {} 评论帖子 {} 时出错: {}", 
                            selectedAgent.getUsername(), selectedPost.getId(), e.getMessage());
                    }
                } else {
                    logger.debug("Agent {} 根据概率决定不评论帖子 {}", selectedAgent.getUsername(), selectedPost.getId());
                }
            }
            
        } catch (Exception e) {
            logger.error("Agent评论现有帖子任务执行出错: {}", e.getMessage(), e);
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
                    
                    // 新增功能：触发其他Agent对新帖子进行评论
                    triggerAgentCommentsOnNewPost(response.getId(), agentConfig.getUsername());
                    
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
    
    /**
     * 触发其他Agent对新帖子进行评论
     * @param postId 新发布的帖子ID
     * @param authorUsername 发帖Agent的用户名（排除自己）
     */
    private void triggerAgentCommentsOnNewPost(Long postId, String authorUsername) {
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
            
            // 为了让评论看起来更自然，随机选择2-4个Agent进行评论
            int commentersCount = Math.min(otherAgents.size(), random.nextInt(3) + 2); // 2-4个评论者
            Collections.shuffle(otherAgents); // 随机打乱顺序
            
            for (int i = 0; i < commentersCount; i++) {
                AgentConfig commenterAgent = otherAgents.get(i);
                User commenterUser = agentUsers.get(commenterAgent.getUsername());
                
                if (commenterUser == null) {
                    logger.warn("评论Agent {} 用户对象未找到，跳过评论", commenterAgent.getUsername());
                    continue;
                }
                
                try {
                    // 随机选择一条评论内容
                    String commentContent = commenterAgent.getComments().get(
                        random.nextInt(commenterAgent.getComments().size()));
                    
                    // 创建评论对象
                    CommentDTO commentDTO = new CommentDTO();
                    commentDTO.setContent(commentContent);
                    commentDTO.setPostId(postId);
                    
                    // 创建Authentication对象
                    Authentication commenterAuth = createAgentAuthentication(commenterUser);
                    
                    // 发布评论 (移除阻塞延迟)
                    commentService.createComment(postId, commentDTO, commenterAuth);
                    logger.info("Agent {} 成功评论了新帖子 {}: {}", 
                        commenterAgent.getUsername(), postId, commentContent);
                        
                } catch (Exception e) {
                    logger.error("Agent {} 评论新帖子 {} 时出错: {}", 
                        commenterAgent.getUsername(), postId, e.getMessage());
                }
            }
            
            logger.info("完成为新帖子 {} 触发Agent评论，共 {} 个评论者", postId, commentersCount);
            
        } catch (Exception e) {
            logger.error("触发Agent评论新帖子时出错: {}", e.getMessage(), e);
        }
    }
} 