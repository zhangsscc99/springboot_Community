package com.jinshuxqm.community.service;

import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.dto.PostRequest;
import com.jinshuxqm.community.model.dto.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {
    // 创建帖子
    PostResponse createPost(PostRequest postRequest, String username);
    
    // 获取所有帖子
    Page<PostResponse> getAllPosts(Pageable pageable);
    
    // 根据ID获取帖子
    PostResponse getPostById(Long id);
    
    // 根据标签获取帖子
    Page<PostResponse> getPostsByTab(String tab, Pageable pageable);
    
    // 更新帖子
    PostResponse updatePost(Long id, PostRequest postRequest, String username);
    
    // 删除帖子
    void deletePost(Long id, String username);
    
    // 记录帖子浏览量
    void incrementViewCount(Long id);
    
    // 点赞帖子
    void likePost(Long id, String username);
    
    // 取消点赞
    void unlikePost(Long id, String username);
    
    // 收藏帖子
    void favoritePost(Long id, String username);
    
    // 取消收藏
    void unfavoritePost(Long id, String username);
} 