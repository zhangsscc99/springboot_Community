package com.jinshuxqm.community.service.impl;

import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.PostStats;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.model.dto.PostRequest;
import com.jinshuxqm.community.model.dto.PostResponse;
import com.jinshuxqm.community.repository.PostRepository;
import com.jinshuxqm.community.repository.UserRepository;
import com.jinshuxqm.community.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;

@Service
public class PostServiceImpl implements PostService {
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
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
        return convertToDto(savedPost);
    }
    
    @Override
    public Page<PostResponse> getAllPosts(Pageable pageable) {
        return postRepository.findAll(pageable)
                .map(this::convertToDto);
    }
    
    @Override
    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("帖子不存在: " + id));
        
        // 增加浏览次数
        incrementViewCount(id);
        
        return convertToDto(post);
    }
    
    @Override
    public Page<PostResponse> getPostsByTab(String tab, Pageable pageable) {
        return postRepository.findByTab(tab, pageable)
                .map(this::convertToDto);
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
        
        return convertToDto(updatedPost);
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
    
    @Override
    public void likePost(Long id, String username) {
        // 此处应实现点赞逻辑
        // 需要一个单独的点赞表记录谁点赞了哪篇帖子
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("帖子不存在: " + id));
        
        if (post.getStats() != null) {
            post.getStats().incrementLikeCount();
            postRepository.save(post);
        }
    }
    
    @Override
    public void unlikePost(Long id, String username) {
        // 取消点赞的逻辑
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("帖子不存在: " + id));
        
        if (post.getStats() != null) {
            post.getStats().decrementLikeCount();
            postRepository.save(post);
        }
    }
    
    @Override
    public void favoritePost(Long id, String username) {
        // 收藏帖子的逻辑
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("帖子不存在: " + id));
        
        if (post.getStats() != null) {
            post.getStats().incrementFavoriteCount();
            postRepository.save(post);
        }
    }
    
    @Override
    public void unfavoritePost(Long id, String username) {
        // 取消收藏的逻辑
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("帖子不存在: " + id));
        
        if (post.getStats() != null) {
            post.getStats().decrementFavoriteCount();
            postRepository.save(post);
        }
    }
    
    // 帮助方法：将实体转换为DTO
    private PostResponse convertToDto(Post post) {
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
        
        return dto;
    }
} 