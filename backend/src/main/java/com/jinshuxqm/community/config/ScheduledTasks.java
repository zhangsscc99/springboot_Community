package com.jinshuxqm.community.config;

import com.jinshuxqm.community.dto.CommentDTO;
import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.model.Role;
import com.jinshuxqm.community.model.ERole;
import com.jinshuxqm.community.repository.PostRepository;
import com.jinshuxqm.community.repository.UserRepository;
import com.jinshuxqm.community.repository.RoleRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 定时任务：模拟多个Agent账号互相交互
 * 每5分钟随机选择一个Agent发布一篇帖子
 * 所有Agent会相互评论、点赞、关注和收藏，创造社区氛围
 */
@Component
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
    private RoleRepository roleRepository;
    
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
            logger.info("📋 Agent配置列表:");
            for (AgentConfig config : agentConfigs) {
                logger.info("  - {} ({}): {}", config.getUsername(), config.getNickname(), config.getEmail());
            }
            
            // 为每个Agent创建账号
            for (AgentConfig config : agentConfigs) {
                // 检查Agent账号是否存在
                Optional<User> existingAgent = userRepository.findByUsername(config.getUsername());
                if (existingAgent.isPresent()) {
                    logger.info("Agent账号 {} 已存在，跳过创建步骤", config.getUsername());
                    User agent = existingAgent.get();
                    agentUsers.put(config.getUsername(), agent);
                    
                    // 更新现有Agent的avatar和其他信息（如果缺失）
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
                        logger.info("更新Agent {} 的信息完成", config.getUsername());
                    }
                } else {
                    // 创建Agent账号
                    User agent = new User();
                    agent.setUsername(config.getUsername());
                    agent.setPassword(passwordEncoder.encode(config.getPassword()));
                    agent.setEmail(config.getEmail());
                    agent.setNickname(config.getNickname());
                    agent.setBio(config.getBio());
                    agent.setAvatar(getDefaultAvatarForAgent(config.getUsername())); // 设置默认头像
                    agent.setCreatedAt(LocalDateTime.now());
                    agent.setUpdatedAt(LocalDateTime.now());
                    
                    // 为Agent设置USER角色
                    Set<Role> roles = new HashSet<>();
                    roleRepository.findByName(ERole.ROLE_USER).ifPresent(roles::add);
                    agent.setRoles(roles);
                    
                    User savedAgent = userRepository.save(agent);
                    agentUsers.put(config.getUsername(), savedAgent);
                    logger.info("Agent账号 {} 创建成功，已设置USER角色", config.getUsername());
                }
            }
            
            logger.info("所有Agent初始化完成，准备开始自动交互");
            logger.info("📊 当前已初始化的Agent用户数量: {}", agentUsers.size());
            logger.info("📋 已初始化的Agent用户列表:");
            for (Map.Entry<String, User> entry : agentUsers.entrySet()) {
                User user = entry.getValue();
                logger.info("  - {} (ID: {}, 昵称: {})", entry.getKey(), user.getId(), user.getNickname());
            }
            
        } catch (Exception e) {
            logger.error("初始化Agent时出错", e);
        }
    }
    
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
            createAgentPost(selectedAgent);
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
                    logger.warn("Agent {} 用户对象未找到，尝试重新获取", selectedAgent.getUsername());
                    // 尝试从数据库重新获取
                    Optional<User> userOpt = userRepository.findByUsername(selectedAgent.getUsername());
                    if (userOpt.isPresent()) {
                        agentUser = userOpt.get();
                        agentUsers.put(selectedAgent.getUsername(), agentUser);
                        logger.info("✅ 成功从数据库重新获取Agent {} 用户对象", selectedAgent.getUsername());
                    } else {
                        logger.error("❌ 数据库中也找不到Agent {} 用户，跳过互动", selectedAgent.getUsername());
                        continue;
                    }
                }
                
                // 创建Authentication对象
                Authentication agentAuth = createAgentAuthentication(agentUser);
                
                // 获取24小时内的最新10篇帖子
                LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
                Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
                List<Post> recentPosts = postRepository.findAll(pageable).getContent()
                    .stream()
                    .filter(post -> post.getCreatedAt().isAfter(twentyFourHoursAgo)) // 只获取24小时内的帖子
                    .limit(10) // 限制为10篇
                    .collect(Collectors.toList());
                
                if (recentPosts.isEmpty()) {
                    logger.info("没有找到24小时内的帖子，跳过互动");
                    continue;
                }
                
                logger.debug("Agent {} 找到 {} 篇24小时内的帖子进行互动", selectedAgent.getUsername(), recentPosts.size());
                
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
     * 每5秒让Agent对24小时内发布的最新帖子进行评论
     * 与立即评论机制配合，避免重复评论
     */
    @Scheduled(fixedRate = 5000) // 每5秒执行一次
    @Transactional // 添加事务注解以避免LazyInitializationException
    public void agentCommentOnExistingPosts() {
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
                    continue;
                }
                
                logger.info("📝 准备为帖子 {} (标题: {}) 补充评论 (当前:{}/期望:{})", 
                    post.getId(), post.getTitle(), currentCommentCount, expectedCommentCount);
                
                // 找出还没有评论的Agent
                List<String> existingCommenterUsernames = existingComments.stream()
                    .map(comment -> comment.getAuthor().getUsername())
                    .collect(Collectors.toList());
                
                List<AgentConfig> uncommmentedAgents = activeAgents.stream()
                    .filter(agent -> !agent.getUsername().equals(post.getAuthor().getUsername())) // 排除作者
                    .filter(agent -> !existingCommenterUsernames.contains(agent.getUsername())) // 排除已评论的
                    .collect(Collectors.toList());
                
                // 让未评论的Agent进行评论
                for (int i = 0; i < uncommmentedAgents.size(); i++) {
                    AgentConfig selectedAgent = uncommmentedAgents.get(i);
                    User agentUser = agentUsers.get(selectedAgent.getUsername());
                    
                                    if (agentUser == null) {
                    logger.warn("Agent {} 用户对象未找到，尝试重新获取", selectedAgent.getUsername());
                    // 尝试从数据库重新获取
                    Optional<User> userOpt = userRepository.findByUsername(selectedAgent.getUsername());
                    if (userOpt.isPresent()) {
                        agentUser = userOpt.get();
                        agentUsers.put(selectedAgent.getUsername(), agentUser);
                        logger.info("✅ 成功从数据库重新获取Agent {} 用户对象", selectedAgent.getUsername());
                    } else {
                        logger.error("❌ 数据库中也找不到Agent {} 用户，跳过评论", selectedAgent.getUsername());
                        continue;
                    }
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
                
                commentedPosts++;
                // 每次只处理一篇帖子，避免过于密集
                if (commentedPosts >= 1) {
                    break;
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
            
            // 先获取帖子信息，确保帖子存在
            final Post finalPost = postRepository.findById(postId).orElse(null);
            if (finalPost == null) {
                logger.warn("帖子 {} 不存在，跳过评论", postId);
                return;
            }
            
            // 让所有其他Agent都评论新帖子 - 使用更短的延迟让评论更快出现
            for (int i = 0; i < otherAgents.size(); i++) {
                AgentConfig commenterAgent = otherAgents.get(i);
                User commenterUser = agentUsers.get(commenterAgent.getUsername());
                
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
     * 小明专属发帖任务 - 每30秒发一次帖子
     */
    @Scheduled(fixedRate = 30000) // 每30秒执行一次
    public void xiaoMingAutoPost() {
        try {
            logger.info("🎯 === 小明专属发帖任务执行 - 当前时间: {} ===", LocalTime.now());
            
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
            
            User xiaoMingUser = agentUsers.get("xiaoming");
            if (xiaoMingUser == null) {
                logger.warn("⚠️ 小明用户对象未找到，尝试重新初始化");
                // 尝试从数据库重新获取
                Optional<User> userOpt = userRepository.findByUsername("xiaoming");
                if (userOpt.isPresent()) {
                    xiaoMingUser = userOpt.get();
                    agentUsers.put("xiaoming", xiaoMingUser);
                    logger.info("✅ 成功从数据库重新获取小明用户对象");
                } else {
                    logger.error("❌ 数据库中也找不到小明用户，跳过发帖");
                    // 尝试重新初始化所有Agent
                    logger.info("🔄 尝试重新初始化所有Agent...");
                    initAgents();
                    return;
                }
            }
            
            logger.info("🚀 小明开始发帖...");
            createAgentPost(xiaoMingConfig);
            logger.info("✅ 小明发帖完成！");
            
        } catch (Exception e) {
            logger.error("❌ 小明发帖任务执行出错: {}", e.getMessage(), e);
        }
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
     * 在新线程中创建评论，确保事务上下文
     */
    private void createCommentInNewTransaction(AgentConfig agentConfig, User agentUser, Post post) {
        try {
            // 创建Authentication对象
            Authentication agentAuth = createAgentAuthentication(agentUser);

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