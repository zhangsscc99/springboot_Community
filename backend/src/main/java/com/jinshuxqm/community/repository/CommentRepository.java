package com.jinshuxqm.community.repository;

import com.jinshuxqm.community.model.Comment;
import com.jinshuxqm.community.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // 获取一级评论（没有父评论）并分页
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parent IS NULL AND c.isDeleted = false ORDER BY c.createdAt DESC")
    Page<Comment> findByPostIdAndParentIsNullOrderByCreatedAtDesc(@Param("postId") Long postId, Pageable pageable);
    
    // 获取评论的回复
    List<Comment> findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(Long parentId);
    
    // 检查评论是否属于指定帖子
    boolean existsByIdAndPostId(Long commentId, Long postId);
    
    // 计算帖子的评论数量
    Long countByPostIdAndIsDeletedFalse(Long postId);
    
    // 查找用户的所有评论
    Page<Comment> findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);
} 