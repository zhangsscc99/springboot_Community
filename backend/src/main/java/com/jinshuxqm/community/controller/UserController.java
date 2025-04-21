package com.jinshuxqm.community.controller;

import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.repository.UserRepository;
import com.jinshuxqm.community.dto.PagedResponseDTO;
import com.jinshuxqm.community.dto.PostDTO;
import com.jinshuxqm.community.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.jinshuxqm.community.exception.ResourceNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    UserRepository userRepository;

    @Autowired
    private PostService postService;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", user.getId());
                    response.put("username", user.getUsername());
                    response.put("email", user.getEmail());
                    response.put("avatar", user.getAvatar());
                    response.put("bio", user.getBio());
                    response.put("createdAt", user.getCreatedAt());
                    
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{userId}/posts")
    public ResponseEntity<?> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "10") int size) {
        
        System.out.println("获取用户ID为 " + userId + " 的帖子");
        
        try {
            // 直接调用服务方法并返回结果
            return ResponseEntity.ok(postService.getPostsByUserId(userId, page, size));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("用户不存在: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("获取用户帖子失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户点赞过的帖子
     */
    @GetMapping("/{userId}/likes")
    public ResponseEntity<?> getUserLikedPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "10") int size) {
        
        System.out.println("获取用户ID为 " + userId + " 点赞的帖子");
        
        try {
            PagedResponseDTO<PostDTO> likedPosts = postService.getLikedPostsByUserId(userId, page, size);
            return ResponseEntity.ok(likedPosts);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("用户不存在: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("获取用户点赞帖子失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户收藏的帖子
     */
    @GetMapping("/{userId}/favorites")
    public ResponseEntity<?> getUserFavoritedPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "10") int size) {
        
        System.out.println("获取用户ID为 " + userId + " 收藏的帖子");
        
        try {
            System.out.println("正在尝试查询用户ID为" + userId + "的收藏帖子");
            PagedResponseDTO<PostDTO> favoritedPosts = postService.getFavoritedPostsByUserId(userId, page, size);
            return ResponseEntity.ok(favoritedPosts);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("用户不存在: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("获取收藏帖子时发生错误: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("获取用户收藏帖子失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户个人资料
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and (authentication.principal.id == #id or hasRole('ADMIN'))")
    public ResponseEntity<?> updateUserProfile(
            @PathVariable Long id,
            @RequestBody Map<String, String> profileData) {
        
        System.out.println("更新用户ID为 " + id + " 的个人资料");
        
        try {
            Optional<User> userOptional = userRepository.findById(id);
            if (!userOptional.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("用户不存在");
            }
            
            User user = userOptional.get();
            
            // 更新头像
            if (profileData.containsKey("avatar")) {
                user.setAvatar(profileData.get("avatar"));
            }
            
            // 更新个人简介
            if (profileData.containsKey("bio")) {
                user.setBio(profileData.get("bio"));
            }
            
            // 更新邮箱（需要验证）
            if (profileData.containsKey("email")) {
                // 在实际应用中，这里应该进行邮箱格式验证和唯一性检查
                user.setEmail(profileData.get("email"));
            }
            
            // 保存更新
            User updatedUser = userRepository.save(user);
            
            // 返回更新后的信息
            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedUser.getId());
            response.put("username", updatedUser.getUsername());
            response.put("email", updatedUser.getEmail());
            response.put("avatar", updatedUser.getAvatar());
            response.put("bio", updatedUser.getBio());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("更新个人资料失败: " + e.getMessage());
        }
    }
} 