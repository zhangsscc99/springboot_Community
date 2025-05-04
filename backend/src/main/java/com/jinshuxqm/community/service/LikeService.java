package com.jinshuxqm.community.service;

import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.PostLike;
import com.jinshuxqm.community.model.PostStats;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.repository.PostRepository;
import com.jinshuxqm.community.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class LikeService {
    
    private static final Logger logger = LoggerFactory.getLogger(LikeService.class);
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 用户点赞帖子
     */
    @Transactional
    public boolean likePost(Long postId, Long userId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (!postOpt.isPresent() || !userOpt.isPresent()) {
            logger.warn("点赞失败: 帖子或用户不存在 (postId={}, userId={})", postId, userId);
            return false;
        }
        
        Post post = postOpt.get();
        User user = userOpt.get();
        
        // 检查用户是否已点赞
        boolean alreadyLiked = post.getLikes().stream()
                .anyMatch(like -> like.getUser().getId().equals(userId));
        
        // 如果用户已经点赞，不做操作
        if (alreadyLiked) {
            return true;
        }
        
        // 创建新的点赞实体
        PostLike postLike = new PostLike();
        postLike.setPost(post);
        postLike.setUser(user);
        
        // 添加点赞到帖子
        post.addLike(postLike);
        
        // 更新PostStats中的点赞计数
        PostStats stats = post.getStats();
        if (stats != null) {
            stats.incrementLikeCount();
        }
        
        // 保存更新
        postRepository.save(post);
        logger.info("用户 {} 点赞了帖子 {}", userId, postId);
        return true;
    }
    
    /**
     * 用户取消点赞帖子
     */
    @Transactional
    public boolean unlikePost(Long postId, Long userId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (!postOpt.isPresent() || !userOpt.isPresent()) {
            logger.warn("取消点赞失败: 帖子或用户不存在 (postId={}, userId={})", postId, userId);
            return false;
        }
        
        Post post = postOpt.get();
        User user = userOpt.get();
        
        // 查找用户的点赞实体
        Optional<PostLike> postLikeOpt = post.getLikes().stream()
                .filter(like -> like.getUser().getId().equals(userId))
                .findFirst();
        
        // 如果用户未点赞，不做操作
        if (!postLikeOpt.isPresent()) {
            return true;
        }
        
        // 移除点赞
        PostLike postLike = postLikeOpt.get();
        post.removeLike(postLike);
        
        // 更新PostStats中的点赞计数
        PostStats stats = post.getStats();
        if (stats != null) {
            stats.decrementLikeCount();
        }
        
        // 保存更新
        postRepository.save(post);
        logger.info("用户 {} 取消了点赞帖子 {}", userId, postId);
        return true;
    }
    
    /**
     * 检查用户是否已点赞帖子
     */
    public boolean hasUserLikedPost(Long postId, Long userId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (!postOpt.isPresent()) {
            return false;
        }
        
        Post post = postOpt.get();
        return post.getLikes().stream()
                .anyMatch(like -> like.getUser().getId().equals(userId));
    }
} 