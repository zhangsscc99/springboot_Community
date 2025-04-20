package com.jinshuxqm.community.repository;

import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.PostFavorite;
import com.jinshuxqm.community.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostFavoriteRepository extends JpaRepository<PostFavorite, Long> {
    
    // 查询用户是否已收藏特定帖子
    Optional<PostFavorite> findByUserAndPost(User user, Post post);
    
    // 获取已收藏特定帖子的用户数量
    long countByPost(Post post);
    
    // 获取用户收藏的所有帖子
    List<PostFavorite> findByUser(User user);
    
    // 删除特定用户对特定帖子的收藏
    void deleteByUserAndPost(User user, Post post);
} 