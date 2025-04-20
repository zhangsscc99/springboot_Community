package com.jinshuxqm.community.service.impl;

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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

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
    
    // 获取指定帖子和用户的操作锁
    private Lock getPostUserLock(Long postId, String username) {
        String key = postId + "-" + username; // 为每个帖子-用户组合创建唯一键
        return postLocks.computeIfAbsent(key, k -> new ReentrantLock());
    }
    
    @Override
    public PostResponse createPost(PostRequest postRequest, String username) {
        // 查找用户
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        
        // 创建帖子实体
        Post post = new Post();
        post.setTitle(postRequest.getTitle());
        post.setContent(postRequest.getContent());
        post.setAuthor(author);
        post.setTab(postRequest.getTab());
        
        if (postRequest.getTags() != null) {
            post.setTags(postRequest.getTags());
        }
        
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        
        // 创建并关联统计信息
        PostStats stats = new PostStats();
        stats.setPost(post);
        post.setStats(stats);
        
        // 保存到数据库
        Post savedPost = postRepository.save(post);
        
        // 转换为响应DTO
        return convertToDto(savedPost, username);
    }
    
    @Override
    public Page<PostResponse> getAllPosts(Pageable pageable) {
        return postRepository.findAll(pageable)
                .map(post -> convertToDto(post, null));
    }
    
    @Override
    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("帖子不存在: " + id));
        
        // 增加浏览次数
        incrementViewCount(id);
        
        return convertToDto(post, null);
    }
    
    @Override
    public Page<PostResponse> getPostsByTab(String tab, Pageable pageable) {
        return postRepository.findByTab(tab, pageable)
                .map(post -> convertToDto(post, null));
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
    public void incrementViewCount(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("帖子不存在: " + id));
        
        if (post.getStats() != null) {
            post.getStats().incrementViewCount();
            postRepository.save(post);
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
            author.getAvatar()
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
} 