package com.jinshuxqm.community.service.impl;

import com.jinshuxqm.community.exception.ResourceNotFoundException;
import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.PostFavorite;
import com.jinshuxqm.community.model.PostLike;
import com.jinshuxqm.community.model.PostStats;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.model.dto.PostRequest;
import com.jinshuxqm.community.model.dto.PostResponse;
import com.jinshuxqm.community.repository.PostFavoriteRepository;
import com.jinshuxqm.community.repository.PostLikeRepository;
import com.jinshuxqm.community.repository.PostRepository;
import com.jinshuxqm.community.repository.UserRepository;
import com.jinshuxqm.community.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.cache.annotation.Cacheable;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import com.jinshuxqm.community.dto.PagedResponseDTO;
import com.jinshuxqm.community.dto.PostDTO;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import org.springframework.data.domain.PageImpl;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PostServiceImpl implements PostService {
    // 使用并发HashMap存储每个帖子的操作锁
    private final ConcurrentHashMap<String, Lock> postLocks = new ConcurrentHashMap<>();
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostLikeRepository postLikeRepository;
    
    @Autowired
    private PostFavoriteRepository postFavoriteRepository;
    
    private static final Logger logger = LoggerFactory.getLogger(PostServiceImpl.class);
    
    // 获取指定帖子和用户的操作锁
    private Lock getPostUserLock(Long postId, String username) {
        String key = postId + "-" + username; // 为每个帖子-用户组合创建唯一键
        return postLocks.computeIfAbsent(key, k -> new ReentrantLock());
    }
    
    @Override
    public PostResponse createPost(PostRequest postRequest, String username) {
        logger.info("➡️ [DEBUG] createPost service method started. User: {}, Tab: {}", username, postRequest.getTab());
        
        try {
            // 查找用户
            User author = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        logger.error("❌ [DEBUG] User not found for username: {}", username);
                        return new UsernameNotFoundException("用户不存在: " + username);
                    });
            
            logger.info("  [DEBUG] Author found: {}", author.getUsername());
            
            // 创建帖子实体
            Post post = new Post();
            post.setTitle(postRequest.getTitle());
            post.setContent(postRequest.getContent());
            post.setAuthor(author);
            post.setTab(postRequest.getTab());
            
            if (postRequest.getTags() != null) {
                post.setTags(postRequest.getTags());
                logger.info("  [DEBUG] Tags set: {}", postRequest.getTags());
            }
            
            post.setCreatedAt(LocalDateTime.now());
            post.setUpdatedAt(LocalDateTime.now());
            
            // 创建并关联统计信息
            PostStats stats = new PostStats();
            stats.setPost(post);
            post.setStats(stats);
            
            logger.info("  [DEBUG] Preparing to save post to database. Title: {}", post.getTitle());
            // 保存到数据库
            Post savedPost = postRepository.save(post);
            logger.info("  [DEBUG] Post successfully saved with ID: {}", savedPost.getId());
            
            // 转换为响应DTO
            PostResponse response = convertToDto(savedPost, username);
            logger.info("⬅️ [DEBUG] createPost service method finished successfully. Post ID: {}", response.getId());
            return response;
        } catch (Exception e) {
            logger.error("❌ [DEBUG] Error during post creation for user {}: {}", username, e.getMessage(), e);
            throw e; // 重新抛出异常以便于上层捕获
        }
    }
    
    @Override
    public Page<PostResponse> getAllPosts(Pageable pageable) {
        return postRepository.findAll(pageable)
                .map(post -> convertToDto(post, null));
    }
    
    @Override
    @Cacheable(value = "posts", key = "#id")
    public PostResponse getPostById(Long id) {
        try {
            // 先获取帖子数据
            Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
            
            // 转换为DTO
            PostResponse response = convertToDto(post, null);
            
            // 异步增加浏览次数，不影响主流程
            CompletableFuture.runAsync(() -> {
                try {
                    incrementViewCount(id);
                } catch (Exception e) {
                    System.err.println("异步增加浏览次数失败: " + e.getMessage());
                }
            });
            
            return response;
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception e) {
            System.err.println("获取帖子详情失败，ID=" + id + ", 错误: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取帖子详情失败", e);
        }
    }
    
    @Override
    public Page<PostResponse> getPostsByTab(String tab, Pageable pageable) {
        System.out.println("=== getPostsByTab调试信息 ===");
        System.out.println("查询tab: '" + tab + "'");
        System.out.println("tab长度: " + tab.length());
        System.out.println("tab字节: " + java.util.Arrays.toString(tab.getBytes()));
        
        Page<Post> posts = postRepository.findByTab(tab, pageable);
        System.out.println("查询到的帖子数量: " + posts.getTotalElements());
        
        if (posts.getTotalElements() > 0) {
            System.out.println("第一个帖子信息:");
            Post firstPost = posts.getContent().get(0);
            System.out.println("  ID: " + firstPost.getId());
            System.out.println("  标题: " + firstPost.getTitle());
            System.out.println("  Tab: '" + firstPost.getTab() + "'");
            System.out.println("  Tab字节: " + java.util.Arrays.toString(firstPost.getTab().getBytes()));
        }
        
        return posts.map(post -> convertToDto(post, null));
    }
    
    @Override
    public PostResponse updatePost(Long id, PostRequest postRequest, String username) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("帖子不存在: " + id));
        
        // 检查权限
        if (!post.getAuthor().getUsername().equals(username)) {
            throw new RuntimeException("无权修改该帖子");
        }
        
        // 更新帖子
        post.setTitle(postRequest.getTitle());
        post.setContent(postRequest.getContent());
        post.setTab(postRequest.getTab());
        
        if (postRequest.getTags() != null) {
            post.setTags(postRequest.getTags());
        }
        
        post.setUpdatedAt(LocalDateTime.now());
        
        Post updatedPost = postRepository.save(post);
        
        return convertToDto(updatedPost, username);
    }
    
    @Override
    public void deletePost(Long id, String username) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("帖子不存在: " + id));
        
        // 检查权限
        if (!post.getAuthor().getUsername().equals(username)) {
            throw new RuntimeException("无权删除该帖子");
        }
        
        postRepository.delete(post);
    }
    
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementViewCount(Long postId) {
        try {
            // 使用乐观锁或悲观锁确保并发安全
            postRepository.incrementViewCount(postId);
        } catch (Exception e) {
            // 仅记录错误，不影响主流程
            System.err.println("增加浏览次数失败: " + e.getMessage());
        }
    }
    
    // 修改点赞方法，添加用户关联
    @Override
    @Transactional
    public void likePost(Long id, String username) {
        Lock lock = getPostUserLock(id, username);
        lock.lock();
        
        try {
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("帖子不存在: " + id));
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + username));
            
            // 检查用户是否已经点赞
            Optional<PostLike> existingLike = postLikeRepository.findByUserAndPost(user, post);
            
            if (existingLike.isPresent()) {
                // 用户已经点赞，不做处理
                return;
            }
            
            // 创建点赞记录
            PostLike like = new PostLike(user, post);
            postLikeRepository.save(like);
            
            // 更新统计数据
            if (post.getStats() != null) {
                post.getStats().incrementLikeCount();
                postRepository.save(post);
            }
        } finally {
            lock.unlock();
        }
    }
    
    // 修改取消点赞方法，删除用户关联
    @Override
    @Transactional
    public void unlikePost(Long id, String username) {
        Lock lock = getPostUserLock(id, username);
        lock.lock();
        
        try {
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("帖子不存在: " + id));
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + username));
            
            // 检查用户是否已经点赞
            Optional<PostLike> existingLike = postLikeRepository.findByUserAndPost(user, post);
            
            if (existingLike.isPresent()) {
                // 删除点赞记录
                postLikeRepository.deleteByUserAndPost(user, post);
                
                // 更新统计数据
                if (post.getStats() != null) {
                    post.getStats().decrementLikeCount();
                    postRepository.save(post);
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    // 修改收藏方法，添加用户关联
    @Override
    @Transactional
    public void favoritePost(Long id, String username) {
        Lock lock = getPostUserLock(id, username);
        lock.lock();
        
        try {
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("帖子不存在: " + id));
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + username));
            
            // 检查用户是否已经收藏
            Optional<PostFavorite> existingFavorite = postFavoriteRepository.findByUserAndPost(user, post);
            
            if (existingFavorite.isPresent()) {
                // 用户已经收藏，不做处理
                return;
            }
            
            // 创建收藏记录
            PostFavorite favorite = new PostFavorite(user, post);
            postFavoriteRepository.save(favorite);
            
            // 更新统计数据
            if (post.getStats() != null) {
                post.getStats().incrementFavoriteCount();
                postRepository.save(post);
            }
        } finally {
            lock.unlock();
        }
    }
    
    // 修改取消收藏方法，删除用户关联
    @Override
    @Transactional
    public void unfavoritePost(Long id, String username) {
        Lock lock = getPostUserLock(id, username);
        lock.lock();
        
        try {
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("帖子不存在: " + id));
            
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + username));
            
            // 检查用户是否已经收藏
            Optional<PostFavorite> existingFavorite = postFavoriteRepository.findByUserAndPost(user, post);
            
            if (existingFavorite.isPresent()) {
                // 删除收藏记录
                postFavoriteRepository.deleteByUserAndPost(user, post);
                
                // 更新统计数据
                if (post.getStats() != null) {
                    post.getStats().decrementFavoriteCount();
                    postRepository.save(post);
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    // 实现新增方法：检查用户是否已点赞帖子
    @Override
    public boolean hasUserLikedPost(Long postId, String username) {
        if (username == null) {
            return false;
        }
        
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("帖子不存在: " + postId));
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + username));
        
        return postLikeRepository.findByUserAndPost(user, post).isPresent();
    }
    
    // 实现新增方法：检查用户是否已收藏帖子
    @Override
    public boolean hasUserFavoritedPost(Long postId, String username) {
        if (username == null) {
            return false;
        }
        
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("帖子不存在: " + postId));
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + username));
        
        return postFavoriteRepository.findByUserAndPost(user, post).isPresent();
    }
    
    // 实现新增方法：获取用户点赞的所有帖子
    @Override
    public List<PostResponse> getLikedPostsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + username));
        
        List<PostLike> likes = postLikeRepository.findByUser(user);
        
        return likes.stream()
                .map(like -> convertToDto(like.getPost(), username))
                .collect(Collectors.toList());
    }
    
    // 实现新增方法：获取用户收藏的所有帖子
    @Override
    public List<PostResponse> getFavoritedPostsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + username));
        
        List<PostFavorite> favorites = postFavoriteRepository.findByUser(user);
        
        return favorites.stream()
                .map(favorite -> convertToDto(favorite.getPost(), username))
                .collect(Collectors.toList());
    }
    
    // 修改convertToDto方法，添加用户的点赞和收藏状态
    private PostResponse convertToDto(Post post, String username) {
        PostResponse dto = new PostResponse();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setTab(post.getTab());
        dto.setTags(post.getTags());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        
        // 设置作者信息
        User author = post.getAuthor();
        PostResponse.UserSummary authorSummary = new PostResponse.UserSummary(
            author.getId(),
            author.getUsername(),
            author.getAvatar(),
            author.getBio()
        );
        dto.setAuthor(authorSummary);
        
        // 设置统计信息
        if (post.getStats() != null) {
            dto.setLikes(post.getStats().getLikeCount());
            dto.setComments(post.getStats().getCommentCount());
            dto.setFavorites(post.getStats().getFavoriteCount());
            dto.setViews(post.getStats().getViewCount());
        }
        
        // 设置当前用户的点赞和收藏状态
        if (username != null) {
            dto.setLikedByCurrentUser(hasUserLikedPost(post.getId(), username));
            dto.setFavoritedByCurrentUser(hasUserFavoritedPost(post.getId(), username));
        } else {
            dto.setLikedByCurrentUser(false);
            dto.setFavoritedByCurrentUser(false);
        }
        
        return dto;
    }
    
    @Override
    public boolean existsById(Long id) {
        return postRepository.existsById(id);
    }

    @Override
    public PagedResponseDTO<PostDTO> getPostsByUserId(Long userId, int page, int size) {
        // 验证用户是否存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        // 创建分页请求
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        // 获取用户的帖子
        Page<Post> postPage = postRepository.findByAuthorId(userId, pageable);
        
        // 转换为 PostDTO 列表
        List<PostDTO> content = postPage.getContent().stream()
                .map(post -> PostDTO.fromEntity(post, null))
                .collect(Collectors.toList());
        
        // 返回分页响应
        return new PagedResponseDTO<>(
                content,
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getTotalElements(),
                postPage.getTotalPages(),
                postPage.isLast()
        );
    }

    @Override
    public PagedResponseDTO<PostDTO> getLikedPostsByUserId(Long userId, int page, int size) {
        // 验证用户是否存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        // 创建分页请求
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        // 使用PostLikeRepository获取用户点赞过的帖子
        Page<Post> likedPosts = postRepository.findPostsLikedByUser(userId, pageable);
        
        // 转换为 PostDTO 列表
        List<PostDTO> content = likedPosts.getContent().stream()
                .map(post -> PostDTO.fromEntity(post, user))
                .collect(Collectors.toList());
        
        // 返回分页响应
        return new PagedResponseDTO<>(
                content,
                likedPosts.getNumber(),
                likedPosts.getSize(),
                likedPosts.getTotalElements(),
                likedPosts.getTotalPages(),
                likedPosts.isLast()
        );
    }

    @Override
    public PagedResponseDTO<PostDTO> getFavoritedPostsByUserId(Long userId, int page, int size) {
        // 验证用户是否存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        // 创建分页请求
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        // 使用Repository获取用户收藏的帖子
        Page<Post> favoritedPosts = postRepository.findPostsFavoritedByUser(userId, pageable);
        
        // 转换为 PostDTO 列表
        List<PostDTO> content = favoritedPosts.getContent().stream()
                .map(post -> PostDTO.fromEntity(post, user))
                .collect(Collectors.toList());
        
        // 返回分页响应
        return new PagedResponseDTO<>(
                content,
                favoritedPosts.getNumber(),
                favoritedPosts.getSize(),
                favoritedPosts.getTotalElements(),
                favoritedPosts.getTotalPages(),
                favoritedPosts.isLast()
        );
    }

    @Override
    public Page<PostResponse> searchPosts(String query, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return new PageImpl<>(new ArrayList<>());
        }
        
        String trimmedQuery = query.trim();
        
        // 检查是否为纯数字ID搜索
        if (trimmedQuery.matches("\\d+")) {
            try {
                Long postId = Long.parseLong(trimmedQuery);
                Post post = postRepository.findById(postId).orElse(null);
                if (post != null) {
                    List<PostResponse> result = List.of(convertToDto(post, null));
                    return new PageImpl<>(result, pageable, 1);
                }
            } catch (NumberFormatException e) {
                // Not a valid Long, proceed to text search
            }
        }
        
        // 否则，执行文本搜索
        Page<Post> posts = postRepository.findByTitleContainingOrContentContaining(trimmedQuery, trimmedQuery, pageable);
        return posts.map(post -> convertToDto(post, null));
    }
    
    /**
     * 检查字符串是否为纯数字
     */
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public Page<PostResponse> getHotPosts(Pageable pageable) {
        // 获取所有帖子
        List<Post> allPosts = postRepository.findAll();
        
        // 当前时间，用于计算时间衰减因子
        LocalDateTime now = LocalDateTime.now();
        
        // 为每个帖子计算热度分数
        List<PostWithScore> postsWithScores = allPosts.stream()
                .map(post -> {
                    // 获取各项统计数据
                    int views = post.getStats() != null ? post.getStats().getViewCount() : 0;
                    int likes = post.getStats() != null ? post.getStats().getLikeCount() : 0;
                    int favorites = post.getStats() != null ? post.getStats().getFavoriteCount() : 0;
                    int comments = post.getStats() != null ? post.getStats().getCommentCount() : 0;
                    
                    // 计算基础热度分数: views*1 + likes*3 + favorites*5 + comments*8
                    double baseScore = views + (likes * 3) + (favorites * 5) + (comments * 8);
                    
                    // 计算时间衰减因子
                    // 使用帖子创建时间与当前时间之间的小时差作为衰减基础
                    double hoursElapsed = 
                        post.getCreatedAt() != null ?
                        java.time.Duration.between(post.getCreatedAt(), now).toHours() : 0;
                    
                    // 使用对数衰减公式: score / (1 + log(1 + hoursElapsed))
                    // 这确保新帖子有较高权重，但随时间推移热度逐渐降低
                    double decayFactor = 1 + Math.log1p(hoursElapsed / 24); // 使用天数，而不是小时
                    double finalScore = baseScore / decayFactor;
                    
                    return new PostWithScore(post, finalScore);
                })
                .sorted(Comparator.comparing(PostWithScore::getScore).reversed()) // 按分数从高到低排序
                .collect(Collectors.toList());
        
        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), postsWithScores.size());
        
        if (start > postsWithScores.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, postsWithScores.size());
        }
        
        List<Post> pagedPosts = postsWithScores.subList(start, end)
                .stream()
                .map(PostWithScore::getPost)
                .collect(Collectors.toList());
        
        // 将Post转换为PostResponse
        List<PostResponse> postResponses = pagedPosts.stream()
                .map(post -> convertToDto(post, null))
                .collect(Collectors.toList());
        
        return new PageImpl<>(postResponses, pageable, postsWithScores.size());
    }
    
    @Override
    public List<Post> getAllPostsForDebug() {
        return postRepository.findAll();
    }
    
    // 帮助类：用于存储帖子及其热度分数
    private static class PostWithScore {
        private final Post post;
        private final double score;
        
        public PostWithScore(Post post, double score) {
            this.post = post;
            this.score = score;
        }
        
        public Post getPost() {
            return post;
        }
        
        public double getScore() {
            return score;
        }
    }
} 