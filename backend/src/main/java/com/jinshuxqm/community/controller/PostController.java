package com.jinshuxqm.community.controller;

import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.dto.PostRequest;
import com.jinshuxqm.community.model.dto.PostResponse;
import com.jinshuxqm.community.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/posts")
public class PostController {

    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

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
        logger.info("开始获取帖子详情，ID: {}", id);
        try {
            logger.info("获取帖子详情，ID: {}", id);
            
            // 使用服务而非直接访问仓库
            PostResponse postResponse = postService.getPostById(id);
            
            logger.info("成功获取帖子详情，ID: {}", id);
            return ResponseEntity.ok(postResponse);
        } catch (Exception e) {
            logger.error("获取帖子详情失败，ID: {}, 错误类型: {}", id, e.getClass().getName());
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

    /**
     * 搜索帖子
     * 
     * @param query 搜索关键词
     * @param page 页码
     * @param size 每页数量
     * @return 匹配的帖子列表
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchPosts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            logger.info("执行搜索，关键词: {}, 页码: {}, 每页数量: {}", query, page, size);
            
            // 调用服务层执行搜索
            Page<Post> postsPage = postService.searchPosts(query, PageRequest.of(page, size));
            
            // 返回简单的结果
            List<Map<String, Object>> results = postsPage.getContent().stream()
                    .map(post -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("id", post.getId());
                        result.put("title", post.getTitle());
                        result.put("content", post.getContent());
                        result.put("created_at", post.getCreatedAt());
                        
                        // 作者信息
                        if (post.getAuthor() != null) {
                            Map<String, Object> author = new HashMap<>();
                            author.put("id", post.getAuthor().getId());
                            author.put("username", post.getAuthor().getUsername());
                            author.put("avatar", post.getAuthor().getAvatar());
                            result.put("author", author);
                        } else {
                            result.put("author", Map.of("id", 0, "username", "未知用户", "avatar", ""));
                        }
                        
                        // 统计信息
                        result.put("likes", post.getStats() != null ? post.getStats().getLikeCount() : 0);
                        result.put("comments", post.getStats() != null ? post.getStats().getCommentCount() : 0);
                        result.put("views", post.getStats() != null ? post.getStats().getViewCount() : 0);
                        
                        return result;
                    })
                    .collect(Collectors.toList());
            
            logger.info("搜索完成，找到 {} 条结果", results.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("搜索失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "搜索失败: " + e.getMessage()));
        }
    }
} 