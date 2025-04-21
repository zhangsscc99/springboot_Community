package com.jinshuxqm.community.controller;

import com.jinshuxqm.community.model.dto.PostRequest;
import com.jinshuxqm.community.model.dto.PostResponse;
import com.jinshuxqm.community.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostService postService;

    // 获取所有帖子
    @GetMapping
    public ResponseEntity<Page<PostResponse>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<PostResponse> posts = postService.getAllPosts(pageable);
        return ResponseEntity.ok(posts);
    }

    // 根据ID获取帖子
    @GetMapping("/{id}")
    public ResponseEntity<?> getPostById(@PathVariable Long id) {
        System.out.println("开始获取帖子详情，ID: " + id);
        try {
            System.out.println("获取帖子详情，ID: " + id);
            
            // 使用服务而非直接访问仓库
            PostResponse postResponse = postService.getPostById(id);
            
            System.out.println("成功获取帖子详情，ID: " + id);
            return ResponseEntity.ok(postResponse);
        } catch (Exception e) {
            System.err.println("获取帖子详情失败，ID: " + id + ", 错误类型: " + e.getClass().getName());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("获取帖子详情失败: " + e.getMessage());
        }
    }

    // 根据标签获取帖子
    @GetMapping("/tab/{tab}")
    public ResponseEntity<Page<PostResponse>> getPostsByTab(
            @PathVariable String tab,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<PostResponse> posts = postService.getPostsByTab(tab, pageable);
        return ResponseEntity.ok(posts);
    }

    // 创建帖子
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest postRequest,
            Authentication authentication) {
        
        String username = authentication.getName();
        PostResponse newPost = postService.createPost(postRequest, username);
        
        return new ResponseEntity<>(newPost, HttpStatus.CREATED);
    }

    // 更新帖子
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostRequest postRequest,
            Authentication authentication) {
        
        String username = authentication.getName();
        PostResponse updatedPost = postService.updatePost(id, postRequest, username);
        
        return ResponseEntity.ok(updatedPost);
    }

    // 删除帖子
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            Authentication authentication) {
        
        String username = authentication.getName();
        postService.deletePost(id, username);
        
        return ResponseEntity.noContent().build();
    }

    // 点赞帖子
    @PostMapping("/{id}/like")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public synchronized ResponseEntity<Void> likePost(
            @PathVariable Long id,
            Authentication authentication) {
        
        String username = authentication.getName();
        postService.likePost(id, username);
        
        return ResponseEntity.ok().build();
    }

    // 取消点赞
    @DeleteMapping("/{id}/like")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public synchronized ResponseEntity<Void> unlikePost(
            @PathVariable Long id,
            Authentication authentication) {
        
        String username = authentication.getName();
        postService.unlikePost(id, username);
        
        return ResponseEntity.ok().build();
    }

    // 收藏帖子
    @PostMapping("/{id}/favorite")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public synchronized ResponseEntity<Void> favoritePost(
            @PathVariable Long id,
            Authentication authentication) {
        
        String username = authentication.getName();
        postService.favoritePost(id, username);
        
        return ResponseEntity.ok().build();
    }

    // 取消收藏
    @DeleteMapping("/{id}/favorite")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public synchronized ResponseEntity<Void> unfavoritePost(
            @PathVariable Long id,
            Authentication authentication) {
        
        String username = authentication.getName();
        postService.unfavoritePost(id, username);
        
        return ResponseEntity.ok().build();
    }
} 