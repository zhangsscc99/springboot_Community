package com.jinshuxqm.community.controller;

import com.jinshuxqm.community.agent.service.AgentManager;
import com.jinshuxqm.community.agent.model.AgentConfig;
import com.jinshuxqm.community.agent.service.AgentInitializationService;
import com.jinshuxqm.community.config.ScheduledTasks;
import com.jinshuxqm.community.dto.CommentDTO;
import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.repository.PostRepository;
import com.jinshuxqm.community.repository.UserRepository;
import com.jinshuxqm.community.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired(required = false)
    private AgentManager agentManager;
    
    @Autowired(required = false)
    private AgentInitializationService agentInitializationService;
    
    @Autowired(required = false)
    private ScheduledTasks scheduledTasks;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello from backend! Server is running at " + LocalTime.now());
    }

    /**
     * 诊断Agent系统状态
     */
    @GetMapping("/agent-status")
    public ResponseEntity<Map<String, Object>> getAgentStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            status.put("serverTime", LocalTime.now().toString());
            status.put("agentManagerInjected", agentManager != null);
            status.put("scheduledTasksInjected", scheduledTasks != null);
            
            if (agentManager != null) {
                List<AgentConfig> agents = agentManager.getAllAgentConfigs();
                status.put("totalAgentsConfigured", agents.size());
                
                // 检查每个Agent的活跃状态
                Map<String, Object> agentDetails = new HashMap<>();
                for (AgentConfig agent : agents) {
                    Map<String, Object> agentInfo = new HashMap<>();
                    agentInfo.put("nickname", agent.getNickname());
                    agentInfo.put("username", agent.getUsername());
                    agentInfo.put("age", agent.getAge());
                    agentInfo.put("postProbability", agent.getPostProbability());
                    agentInfo.put("activeStartTime", agent.getActiveStartTime().toString());
                    agentInfo.put("activeEndTime", agent.getActiveEndTime().toString());
                    agentInfo.put("isActiveNow", agent.isActiveNow());
                    agentDetails.put(agent.getUsername(), agentInfo);
                }
                status.put("agentDetails", agentDetails);
                
                // 统计活跃Agent数量
                long activeAgentsCount = agents.stream()
                    .filter(AgentConfig::isActiveNow)
                    .count();
                status.put("activeAgentsCount", activeAgentsCount);
            } else {
                status.put("error", "AgentManager not injected");
            }
            
        } catch (Exception e) {
            status.put("error", "Exception occurred: " + e.getMessage());
        }
        
        return ResponseEntity.ok(status);
    }

    /**
     * 手动触发Agent发帖测试
     */
    @PostMapping("/trigger-agent-post")
    public ResponseEntity<Map<String, Object>> triggerAgentPost() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (scheduledTasks == null) {
                result.put("error", "ScheduledTasks not injected");
                return ResponseEntity.ok(result);
            }
            
            if (agentManager == null) {
                result.put("error", "AgentManager not injected");
                return ResponseEntity.ok(result);
            }
            
            // 手动调用autoPost方法
            scheduledTasks.autoPost();
            result.put("success", true);
            result.put("message", "Agent post triggered manually");
            result.put("timestamp", LocalTime.now().toString());
            
        } catch (Exception e) {
            result.put("error", "Failed to trigger agent post: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 手动触发小明发帖测试
     */
    @PostMapping("/trigger-xiaoming-post")
    public ResponseEntity<Map<String, Object>> triggerXiaoMingPost() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (scheduledTasks == null) {
                result.put("error", "ScheduledTasks not injected");
                return ResponseEntity.ok(result);
            }
            
            if (agentManager == null) {
                result.put("error", "AgentManager not injected");
                return ResponseEntity.ok(result);
            }
            
            // 手动调用小明的发帖方法
            scheduledTasks.xiaoMingAutoPost();
            result.put("success", true);
            result.put("message", "小明发帖任务手动触发成功");
            result.put("timestamp", LocalTime.now().toString());
            
        } catch (Exception e) {
            result.put("error", "Failed to trigger 小明 post: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 手动触发agent评论任务
     */
    @GetMapping("/trigger-agent-comments")
    public ResponseEntity<?> triggerAgentComments() {
        try {
            // 手动调用agent评论方法
            scheduledTasks.agentCommentOnExistingPosts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Agent评论任务已触发");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "触发Agent评论任务失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取关注栏目的帖子
     */
    @GetMapping("/follow-posts")
    public ResponseEntity<?> getFollowPosts() {
        try {
            List<Post> posts = postRepository.findByTab("关注", 
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", posts.size());
            response.put("posts", posts);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "获取关注栏目帖子失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取指定帖子的评论
     */
    @GetMapping("/post/{postId}/comments")
    public ResponseEntity<?> getPostComments(@PathVariable Long postId) {
        try {
            List<CommentDTO> comments = commentService.getCommentsByPostId(postId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("postId", postId);
            response.put("commentCount", comments.size());
            response.put("comments", comments);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "获取评论失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取所有Agent用户
     */
    @GetMapping("/agents")
    public ResponseEntity<?> getAgents() {
        try {
            List<User> agents = userRepository.findByUsernameIn(
                List.of("city_girl", "career_sister", "teen_heart", "family_man", "lovelessboy"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("agentCount", agents.size());
            response.put("agents", agents);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "获取Agent用户失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 手动触发agent对当天推荐栏目帖子的评论
     */
    @GetMapping("/trigger-today-comments")
    public ResponseEntity<?> triggerTodayComments() {
        try {
            // 手动调用agent评论方法
            scheduledTasks.agentCommentOnExistingPosts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Agent对当天推荐栏目帖子评论任务已触发");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "触发Agent评论任务失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取推荐栏目当天的帖子
     */
    @GetMapping("/recommend-today-posts")
    public ResponseEntity<?> getRecommendTodayPosts() {
        try {
            java.time.LocalDateTime startOfDay = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            java.time.LocalDateTime endOfDay = java.time.LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            
            List<Post> posts = postRepository.findByTab("推荐", 
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent()
                .stream()
                .filter(post -> post.getCreatedAt().isAfter(startOfDay) && post.getCreatedAt().isBefore(endOfDay))
                .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", posts.size());
            response.put("posts", posts);
            response.put("message", "推荐栏目当天帖子列表");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "获取推荐栏目当天帖子失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 手动触发Agent发帖并立即评论测试
     */
    @GetMapping("/trigger-agent-post-and-comment")
    public ResponseEntity<?> triggerAgentPostAndComment() {
        try {
            // 手动调用agent发帖方法
            scheduledTasks.autoPost();
            
            // 等待一下让评论完成
            Thread.sleep(3000);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Agent发帖和评论任务已触发，请检查推荐栏目");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "触发Agent发帖和评论任务失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 测试评论显示功能
     */
    @GetMapping("/test-comment-display/{postId}")
    public ResponseEntity<?> testCommentDisplay(@PathVariable Long postId) {
        try {
            // 直接调用评论服务获取评论
            List<CommentDTO> comments = commentService.getCommentsByPostId(postId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("postId", postId);
            response.put("commentCount", comments.size());
            response.put("comments", comments);
            response.put("message", "评论获取成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "测试评论显示失败: " + e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 检查Agent用户在数据库中的存在情况
     */
    @GetMapping("/check-agent-users")
    public ResponseEntity<Map<String, Object>> checkAgentUsers() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (agentManager == null) {
                result.put("error", "AgentManager not injected");
                return ResponseEntity.ok(result);
            }
            
            List<AgentConfig> agents = agentManager.getAllAgentConfigs();
            result.put("totalAgentsConfigured", agents.size());
            
            Map<String, Object> agentUsersStatus = new HashMap<>();
            int existingUsersCount = 0;
            
            for (AgentConfig agent : agents) {
                Map<String, Object> agentStatus = new HashMap<>();
                agentStatus.put("username", agent.getUsername());
                agentStatus.put("nickname", agent.getNickname());
                
                // 检查用户是否存在于数据库中
                User user = userRepository.findByUsername(agent.getUsername()).orElse(null);
                if (user != null) {
                    existingUsersCount++;
                    agentStatus.put("existsInDatabase", true);
                    agentStatus.put("userId", user.getId());
                    agentStatus.put("email", user.getEmail());
                    agentStatus.put("hasRoles", user.getRoles() != null && !user.getRoles().isEmpty());
                    agentStatus.put("avatar", user.getAvatar());
                    agentStatus.put("bio", user.getBio());
                    agentStatus.put("createdAt", user.getCreatedAt().toString());
                } else {
                    agentStatus.put("existsInDatabase", false);
                    agentStatus.put("error", "User not found in database");
                }
                
                agentUsersStatus.put(agent.getUsername(), agentStatus);
            }
            
            result.put("existingUsersCount", existingUsersCount);
            result.put("missingUsersCount", agents.size() - existingUsersCount);
            result.put("agentUsersStatus", agentUsersStatus);
            result.put("timestamp", LocalTime.now().toString());
            
        } catch (Exception e) {
            result.put("error", "Exception occurred: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 手动重新初始化Agent用户
     */
    @PostMapping("/reinit-agents")
    public ResponseEntity<Map<String, Object>> reinitAgents() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (scheduledTasks == null) {
                result.put("error", "ScheduledTasks not injected");
                return ResponseEntity.ok(result);
            }
            
            // 手动调用Agent初始化方法
            if (agentInitializationService != null) {
                agentInitializationService.reinitAllAgents();
                result.put("success", true);
                result.put("message", "Agent重新初始化完成");
            } else {
                result.put("success", false);
                result.put("message", "AgentInitializationService未可用");
            }
            result.put("timestamp", LocalTime.now().toString());
            
        } catch (Exception e) {
            result.put("error", "Failed to reinit agents: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 专门检查小明Agent的状态
     */
    @GetMapping("/check-xiaoming")
    public ResponseEntity<Map<String, Object>> checkXiaoMing() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 检查Agent配置
            if (agentManager != null) {
                AgentConfig xiaoMingConfig = agentManager.getAgentConfigByUsername("xiaoming");
                if (xiaoMingConfig != null) {
                    result.put("configExists", true);
                    result.put("username", xiaoMingConfig.getUsername());
                    result.put("nickname", xiaoMingConfig.getNickname());
                    result.put("isActiveNow", xiaoMingConfig.isActiveNow());
                    result.put("postProbability", xiaoMingConfig.getPostProbability());
                } else {
                    result.put("configExists", false);
                    result.put("error", "小明的Agent配置未找到");
                }
            } else {
                result.put("error", "AgentManager未注入");
            }
            
            // 检查数据库中的用户
            User xiaoMingUser = userRepository.findByUsername("xiaoming").orElse(null);
            if (xiaoMingUser != null) {
                result.put("userExistsInDB", true);
                result.put("userId", xiaoMingUser.getId());
                result.put("userEmail", xiaoMingUser.getEmail());
                result.put("userNickname", xiaoMingUser.getNickname());
                result.put("userHasRoles", xiaoMingUser.getRoles() != null && !xiaoMingUser.getRoles().isEmpty());
                result.put("userCreatedAt", xiaoMingUser.getCreatedAt().toString());
            } else {
                result.put("userExistsInDB", false);
                result.put("dbError", "小明用户在数据库中未找到");
            }
            
            result.put("timestamp", LocalTime.now().toString());
            
        } catch (Exception e) {
            result.put("error", "检查小明状态时出错: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 测试获取24小时内的最新帖子
     */
    @GetMapping("/check-recent-posts")
    public ResponseEntity<Map<String, Object>> checkRecentPosts() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取24小时内发布的最新帖子
            LocalTime now = LocalTime.now();
            java.time.LocalDateTime twentyFourHoursAgo = java.time.LocalDateTime.now().minusHours(24);
            java.time.LocalDateTime fiveMinutesAgo = java.time.LocalDateTime.now().minusMinutes(5);
            
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(0, 30, 
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
            
            List<com.jinshuxqm.community.model.Post> recentPosts = postRepository.findAll(pageable).getContent()
                .stream()
                .filter(post -> post.getCreatedAt().isAfter(twentyFourHoursAgo))
                .filter(post -> post.getCreatedAt().isBefore(fiveMinutesAgo))
                .collect(java.util.stream.Collectors.toList());
            
            result.put("totalRecentPosts", recentPosts.size());
            result.put("queryTime", now.toString());
            result.put("timeRange", "过去24小时（排除最近5分钟）");
            
            if (!recentPosts.isEmpty()) {
                com.jinshuxqm.community.model.Post latestPost = recentPosts.get(0);
                com.jinshuxqm.community.model.Post oldestPost = recentPosts.get(recentPosts.size() - 1);
                
                result.put("latestPostId", latestPost.getId());
                result.put("latestPostTitle", latestPost.getTitle());
                result.put("latestPostTime", latestPost.getCreatedAt().toString());
                result.put("oldestPostId", oldestPost.getId());
                result.put("oldestPostTitle", oldestPost.getTitle());
                result.put("oldestPostTime", oldestPost.getCreatedAt().toString());
                
                // 显示前5个帖子的详情
                List<Map<String, Object>> postDetails = new ArrayList<>();
                for (int i = 0; i < Math.min(5, recentPosts.size()); i++) {
                    com.jinshuxqm.community.model.Post post = recentPosts.get(i);
                    Map<String, Object> postInfo = new HashMap<>();
                    postInfo.put("id", post.getId());
                    postInfo.put("title", post.getTitle());
                    postInfo.put("author", post.getAuthor().getUsername());
                    postInfo.put("tab", post.getTab());
                    postInfo.put("createdAt", post.getCreatedAt().toString());
                    
                    // 获取评论数
                    try {
                        List<com.jinshuxqm.community.dto.CommentDTO> comments = 
                            commentService.getCommentsByPostId(post.getId());
                        postInfo.put("commentCount", comments.size());
                    } catch (Exception e) {
                        postInfo.put("commentCount", "获取失败");
                    }
                    
                    postDetails.add(postInfo);
                }
                result.put("samplePosts", postDetails);
            }
            
        } catch (Exception e) {
            result.put("error", "获取最新帖子时出错: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 手动触发Agent对最新帖子的评论任务
     */
    @PostMapping("/trigger-recent-comments")
    public ResponseEntity<Map<String, Object>> triggerRecentComments() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (scheduledTasks == null) {
                result.put("error", "ScheduledTasks not injected");
                return ResponseEntity.ok(result);
            }
            
            // 手动调用Agent评论最新帖子的方法
            scheduledTasks.agentCommentOnExistingPosts();
            result.put("success", true);
            result.put("message", "Agent评论最新帖子任务手动触发成功");
            result.put("timestamp", LocalTime.now().toString());
            
        } catch (Exception e) {
            result.put("error", "Failed to trigger recent comments: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 详细检查小明发帖系统状态
     */
    @GetMapping("/debug-xiaoming-system")
    public ResponseEntity<Map<String, Object>> debugXiaoMingSystem() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("currentTime", LocalTime.now().toString());
            result.put("timestamp", java.time.LocalDateTime.now().toString());
            
            // 1. 检查定时任务系统
            result.put("scheduledTasksInjected", scheduledTasks != null);
            
            // 2. 检查AgentManager
            result.put("agentManagerInjected", agentManager != null);
            
            if (agentManager != null) {
                // 3. 检查小明的Agent配置
                AgentConfig xiaoMingConfig = agentManager.getAgentConfigByUsername("xiaoming");
                if (xiaoMingConfig != null) {
                    Map<String, Object> configInfo = new HashMap<>();
                    configInfo.put("exists", true);
                    configInfo.put("username", xiaoMingConfig.getUsername());
                    configInfo.put("nickname", xiaoMingConfig.getNickname());
                    configInfo.put("postProbability", xiaoMingConfig.getPostProbability());
                    configInfo.put("isActiveNow", xiaoMingConfig.isActiveNow());
                    configInfo.put("activeStartTime", xiaoMingConfig.getActiveStartTime().toString());
                    configInfo.put("activeEndTime", xiaoMingConfig.getActiveEndTime().toString());
                    configInfo.put("postTitlesCount", xiaoMingConfig.getPostTitles() != null ? xiaoMingConfig.getPostTitles().size() : 0);
                    configInfo.put("postContentsCount", xiaoMingConfig.getPostContents() != null ? xiaoMingConfig.getPostContents().size() : 0);
                    result.put("xiaoMingConfig", configInfo);
                } else {
                    result.put("xiaoMingConfig", Map.of("exists", false, "error", "小明配置未找到"));
                }
                
                // 4. 检查所有Agent配置
                List<AgentConfig> allConfigs = agentManager.getAllAgentConfigs();
                result.put("totalAgentConfigs", allConfigs.size());
                List<String> allAgentNames = new ArrayList<>();
                for (AgentConfig config : allConfigs) {
                    allAgentNames.add(config.getUsername() + "(" + config.getNickname() + ")");
                }
                result.put("allAgentNames", allAgentNames);
            }
            
            // 5. 检查数据库中的小明用户
            User xiaoMingUser = userRepository.findByUsername("xiaoming").orElse(null);
            if (xiaoMingUser != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("exists", true);
                userInfo.put("id", xiaoMingUser.getId());
                userInfo.put("username", xiaoMingUser.getUsername());
                userInfo.put("nickname", xiaoMingUser.getNickname());
                userInfo.put("email", xiaoMingUser.getEmail());
                userInfo.put("hasRoles", xiaoMingUser.getRoles() != null && !xiaoMingUser.getRoles().isEmpty());
                userInfo.put("createdAt", xiaoMingUser.getCreatedAt().toString());
                result.put("xiaoMingUser", userInfo);
            } else {
                result.put("xiaoMingUser", Map.of("exists", false, "error", "小明用户在数据库中未找到"));
            }
            
            // 6. 检查Spring定时任务是否启用
            try {
                java.lang.reflect.Method method = scheduledTasks.getClass().getDeclaredMethod("xiaoMingAutoPost");
                org.springframework.scheduling.annotation.Scheduled scheduled = method.getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);
                if (scheduled != null) {
                    Map<String, Object> schedulingInfo = new HashMap<>();
                    schedulingInfo.put("methodExists", true);
                    schedulingInfo.put("fixedRate", scheduled.fixedRate());
                    schedulingInfo.put("fixedRateString", scheduled.fixedRateString());
                    result.put("schedulingAnnotation", schedulingInfo);
                } else {
                    result.put("schedulingAnnotation", Map.of("exists", false, "error", "@Scheduled注解未找到"));
                }
            } catch (Exception e) {
                result.put("schedulingAnnotation", Map.of("error", "检查@Scheduled注解时出错: " + e.getMessage()));
            }
            
        } catch (Exception e) {
            result.put("error", "系统检查时出错: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 强制执行小明发帖并显示详细日志
     */
    @PostMapping("/force-xiaoming-post")
    public ResponseEntity<Map<String, Object>> forceXiaoMingPost() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("startTime", LocalTime.now().toString());
            
            if (scheduledTasks == null) {
                result.put("error", "ScheduledTasks未注入");
                return ResponseEntity.ok(result);
            }
            
            if (agentManager == null) {
                result.put("error", "AgentManager未注入");
                return ResponseEntity.ok(result);
            }
            
            // 强制执行小明发帖
            logger.info("🔥 === 手动强制执行小明发帖 ===");
            scheduledTasks.xiaoMingAutoPost();
            
            result.put("success", true);
            result.put("message", "小明发帖任务强制执行完成");
            result.put("endTime", LocalTime.now().toString());
            result.put("note", "请查看服务器日志获取详细信息");
            
        } catch (Exception e) {
            result.put("error", "强制执行小明发帖时出错: " + e.getMessage());
            logger.error("强制执行小明发帖时出错", e);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 测试帖子ID搜索功能
     */
    @GetMapping("/test-id-search")
    public ResponseEntity<Map<String, Object>> testIdSearch(
            @RequestParam(defaultValue = "1") String testId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            result.put("timestamp", LocalTime.now().toString());
            result.put("testId", testId);
            
            // 1. 测试是否为数字
            boolean isNumeric = testId.matches("\\d+");
            result.put("isNumeric", isNumeric);
            
            if (isNumeric) {
                try {
                    Long postId = Long.parseLong(testId);
                    result.put("parsedId", postId);
                    
                    // 2. 检查帖子是否存在
                    Optional<Post> post = postRepository.findById(postId);
                    if (post.isPresent()) {
                        Post foundPost = post.get();
                        result.put("postFound", true);
                        result.put("post", Map.of(
                            "id", foundPost.getId(),
                            "title", foundPost.getTitle(),
                            "author", foundPost.getAuthor().getUsername(),
                            "createdAt", foundPost.getCreatedAt().toString()
                        ));
                    } else {
                        result.put("postFound", false);
                        result.put("message", "未找到ID为 " + postId + " 的帖子");
                    }
                } catch (NumberFormatException e) {
                    result.put("error", "无法解析为有效的帖子ID: " + e.getMessage());
                }
            } else {
                result.put("message", "输入不是纯数字，将执行关键词搜索");
            }
            
            result.put("success", true);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "测试ID搜索出错: " + e.getMessage());
            logger.error("测试ID搜索出错", e);
        }
        
        return ResponseEntity.ok(result);
    }
}