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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import com.jinshuxqm.community.exception.ResourceNotFoundException;

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

    // 获取热门帖子
    @GetMapping("/hot")
    public ResponseEntity<Page<PostResponse>> getHotPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        logger.info("获取热门帖子，页码：{}，大小：{}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> hotPosts = postService.getHotPosts(pageable);
        
        return ResponseEntity.ok(hotPosts);
    }

    // 根据标签获取帖子
    @GetMapping("/tab/{tab}")
    public ResponseEntity<Page<PostResponse>> getPostsByTab(
            @PathVariable String tab,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        System.out.println("=== PostController调试信息 ===");
        System.out.println("接收到的tab参数: '" + tab + "'");
        try {
            System.out.println("URL编码检查: " + URLDecoder.decode(tab, StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println("URL解码失败: " + e.getMessage());
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostResponse> posts;
        
        // 如果是热榜标签，使用热门帖子逻辑
        if ("热榜".equals(tab)) {
            logger.info("通过热榜标签获取热门帖子");
            posts = postService.getHotPosts(pageable);
        } else {
            posts = postService.getPostsByTab(tab, pageable);
        }
        
        System.out.println("返回帖子数量: " + posts.getTotalElements());
        
        return ResponseEntity.ok(posts);
    }
    
    // 临时调试接口：查看所有帖子的tab分布
    @GetMapping("/debug/tabs")
    public ResponseEntity<?> debugTabs() {
        try {
            // 这需要在PostService中添加对应方法
            List<Post> allPosts = postService.getAllPostsForDebug();
            
            Map<String, Long> tabCounts = allPosts.stream()
                .collect(Collectors.groupingBy(
                    post -> post.getTab() == null ? "NULL" : post.getTab(),
                    Collectors.counting()
                ));
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("总帖子数", allPosts.size());
            debugInfo.put("tab分布", tabCounts);
            
            // 显示最近的10个帖子
            List<Map<String, Object>> recentPosts = allPosts.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(10)
                .map(post -> {
                    Map<String, Object> postInfo = new HashMap<>();
                    postInfo.put("id", post.getId());
                    postInfo.put("title", post.getTitle());
                    postInfo.put("tab", post.getTab());
                    postInfo.put("author", post.getAuthor().getUsername());
                    postInfo.put("createdAt", post.getCreatedAt());
                    return postInfo;
                })
                .collect(Collectors.toList());
            
            debugInfo.put("最近10个帖子", recentPosts);
            
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("调试查询失败: " + e.getMessage());
        }
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
            // 检查是否为ID搜索
            if (query != null && query.trim().matches("\\d+")) {
                Long postId = Long.parseLong(query.trim());
                logger.info("执行帖子ID搜索，ID: {}", postId);
                
                try {
                    PostResponse postResponse = postService.getPostById(postId);
                    // 即使是单个结果，也用列表包装以便前端统一处理
                    return ResponseEntity.ok(List.of(postResponse));
                } catch (ResourceNotFoundException e) {
                    logger.info("ID搜索完成，未找到ID为 {} 的帖子", postId);
                    return ResponseEntity.ok(List.of()); // 返回空列表
                }
            }
            
            // 如果不是ID，则执行关键词搜索
            logger.info("执行关键词搜索，关键词: {}, 页码: {}, 每页数量: {}", query, page, size);
            Page<PostResponse> postsPage = postService.searchPosts(query, PageRequest.of(page, size));
            
            logger.info("关键词搜索完成，找到 {} 条结果", postsPage.getTotalElements());
            return ResponseEntity.ok(postsPage.getContent());
            
        } catch (Exception e) {
            logger.error("搜索失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "搜索失败: " + e.getMessage()));
        }
    }
} 