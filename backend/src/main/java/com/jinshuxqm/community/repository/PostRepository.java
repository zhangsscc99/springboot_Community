package com.jinshuxqm.community.repository;

import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    // 根据标签查询帖子
    Page<Post> findByTab(String tab, Pageable pageable);
    
    // 根据作者查询帖子
    List<Post> findByAuthor(User author);
    
    // 根据标签列表查询帖子
    Page<Post> findByTagsContaining(String tag, Pageable pageable);
    
    // 根据标题查询帖子
    Optional<Post> findByTitle(String title);

    // 添加此方法
    Page<Post> findByAuthorId(Long authorId, Pageable pageable);
} 