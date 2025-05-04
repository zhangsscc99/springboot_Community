package com.jinshuxqm.community.repository;

import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

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

    // 查询用户点赞的帖子
    @Query("SELECT DISTINCT p FROM Post p JOIN p.likes l WHERE l.user.id = :userId")
    Page<Post> findPostsLikedByUser(@Param("userId") Long userId, Pageable pageable);

    // 查询用户收藏的帖子
    @Query("SELECT DISTINCT p FROM Post p JOIN p.favorites f WHERE f.user.id = :userId")
    Page<Post> findPostsFavoritedByUser(@Param("userId") Long userId, Pageable pageable);

    // 添加此方法
    @Modifying
    @Transactional
    @Query("UPDATE PostStats ps SET ps.viewCount = ps.viewCount + 1 WHERE ps.post.id = :postId")
    int incrementViewCount(@Param("postId") Long postId);

    // 搜索帖子
    @Query("SELECT p FROM Post p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Post> searchByTitleOrContent(@Param("query") String query, Pageable pageable);

    /**
     * 更新帖子的点赞数量
     */
    @Modifying
    @Transactional
    @Query("UPDATE PostStats ps SET ps.likeCount = :likesCount WHERE ps.post.id = :postId")
    void updateLikes(@Param("postId") Long postId, @Param("likesCount") int likesCount);

    /**
     * 更新帖子的评论数量
     */
    @Modifying
    @Transactional
    @Query("UPDATE PostStats ps SET ps.commentCount = :commentsCount WHERE ps.post.id = :postId")
    void updateComments(@Param("postId") Long postId, @Param("commentsCount") int commentsCount);

    /**
     * 更新帖子的浏览数量
     */
    @Modifying
    @Transactional
    @Query("UPDATE PostStats ps SET ps.viewCount = :viewsCount WHERE ps.post.id = :postId")
    void updateViews(@Param("postId") Long postId, @Param("viewsCount") int viewsCount);
} 