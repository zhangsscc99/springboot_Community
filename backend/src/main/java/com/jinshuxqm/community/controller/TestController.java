package com.jinshuxqm.community.controller;

import com.jinshuxqm.community.config.ScheduledTasks;
import com.jinshuxqm.community.dto.CommentDTO;
import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.repository.PostRepository;
import com.jinshuxqm.community.repository.UserRepository;
import com.jinshuxqm.community.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TestController {

    @Autowired
    private ScheduledTasks scheduledTasks;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private UserRepository userRepository;

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
}