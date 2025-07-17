package com.jinshuxqm.community.controller;

import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.security.services.UserDetailsImpl;
import com.jinshuxqm.community.service.UserFollowService;
import com.jinshuxqm.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserFollowController {

    @Autowired
    private UserFollowService userFollowService;
    
    @Autowired
    private UserService userService;
    
    /**
     * Follow a user
     * @param userId the ID of the user to follow
     * @return a response with success status and a message
     */
    @PostMapping("/{userId}/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> followUser(@PathVariable Long userId) {
        try {
            // Get the current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            Long currentUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                currentUserId = userDetails.getId();
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无法获取当前用户信息");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Follow the user
            boolean success = userFollowService.followUser(currentUserId, userId);
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("success", true);
                response.put("message", "User followed successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Could not follow user");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            System.err.println("关注用户时发生错误: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "关注用户失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Unfollow a user
     * @param userId the ID of the user to unfollow
     * @return a response with success status and a message
     */
    @DeleteMapping("/{userId}/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> unfollowUser(@PathVariable Long userId) {
        try {
            // Get the current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            Long currentUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                currentUserId = userDetails.getId();
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "无法获取当前用户信息");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Unfollow the user
            boolean success = userFollowService.unfollowUser(currentUserId, userId);
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("success", true);
                response.put("message", "User unfollowed successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Could not unfollow user");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            System.err.println("取消关注用户时发生错误: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "取消关注用户失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Check if the current user is following another user
     * @param userId the ID of the user to check
     * @return a response with the following status
     */
    @GetMapping("/{userId}/follow/check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkFollowing(@PathVariable Long userId) {
        try {
            // Get the current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // 安全地获取当前用户ID
            Long currentUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                currentUserId = userDetails.getId();
            } else {
                // 如果无法获取用户信息，返回未关注状态
                Map<String, Object> response = new HashMap<>();
                response.put("following", false);
                response.put("error", "无法获取当前用户信息");
                return ResponseEntity.ok(response);
            }
            
            // Check if following
            boolean isFollowing = userFollowService.isFollowing(currentUserId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("following", isFollowing);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 发生异常时返回未关注状态
            System.err.println("检查关注状态时发生错误: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("following", false);
            response.put("error", "检查关注状态失败");
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Get the followers of a user
     * @param userId the ID of the user
     * @return a list of users who follow the given user
     */
    @GetMapping("/{userId}/followers")
    public ResponseEntity<?> getFollowers(@PathVariable Long userId) {
        List<User> followers = userFollowService.getFollowers(userId);
        // Convert to simple user DTOs to avoid circular references
        List<Map<String, Object>> followerDtos = followers.stream()
                .map(user -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", user.getId());
                    dto.put("username", user.getUsername());
                    dto.put("avatar", user.getAvatar());
                    dto.put("bio", user.getBio());
                    return dto;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("followers", followerDtos);
        response.put("count", followerDtos.size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get the users that a user follows
     * @param userId the ID of the user
     * @return a list of users that the given user follows
     */
    @GetMapping("/{userId}/following")
    public ResponseEntity<?> getFollowing(@PathVariable Long userId) {
        List<User> following = userFollowService.getFollowing(userId);
        // Convert to simple user DTOs to avoid circular references
        List<Map<String, Object>> followingDtos = following.stream()
                .map(user -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", user.getId());
                    dto.put("username", user.getUsername());
                    dto.put("avatar", user.getAvatar());
                    dto.put("bio", user.getBio());
                    return dto;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("following", followingDtos);
        response.put("count", followingDtos.size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get the follow counts for a user
     * @param userId the ID of the user
     * @return the number of followers and following
     */
    @GetMapping("/{userId}/follow/count")
    public ResponseEntity<?> getFollowCounts(@PathVariable Long userId) {
        Long followerCount = userFollowService.countFollowers(userId);
        Long followingCount = userFollowService.countFollowing(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("followerCount", followerCount);
        response.put("followingCount", followingCount);
        return ResponseEntity.ok(response);
    }
} 