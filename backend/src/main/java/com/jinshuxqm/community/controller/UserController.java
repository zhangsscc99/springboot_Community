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
} 