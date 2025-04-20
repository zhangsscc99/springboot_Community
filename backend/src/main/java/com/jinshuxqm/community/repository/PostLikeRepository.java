package com.jinshuxqm.community.repository;

import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.PostLike;
import com.jinshuxqm.community.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    
    // 查询用户是否已点赞特定帖子
    Optional<PostLike> findByUserAndPost(User user, Post post);
    
    // 获取已点赞特定帖子的用户数量
    long countByPost(Post post);
    
    // 获取用户点赞的所有帖子
    List<PostLike> findByUser(User user);
    
    // 删除特定用户对特定帖子的点赞
    void deleteByUserAndPost(User user, Post post);
} 