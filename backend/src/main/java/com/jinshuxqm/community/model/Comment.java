package com.jinshuxqm.community.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> replies = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_user_id")
    private User replyToUser;
    
    @ManyToMany
    @JoinTable(
        name = "comment_likes",
        joinColumns = @JoinColumn(name = "comment_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> likedByUsers = new HashSet<>();
    
    @Column(nullable = false)
    private Integer likes = 0;
    
    @Column(nullable = false)
    private Boolean isDeleted = false;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // 添加点赞方法
    public void addLike(User user) {
        this.likedByUsers.add(user);
        this.likes = this.likedByUsers.size();
    }
    
    // 取消点赞方法
    public void removeLike(User user) {
        this.likedByUsers.remove(user);
        this.likes = this.likedByUsers.size();
    }
    
    // 检查是否被用户点赞
    public boolean isLikedByUser(User user) {
        return this.likedByUsers.contains(user);
    }
    
    // 添加回复方法
    public void addReply(Comment reply) {
        replies.add(reply);
        reply.setParent(this);
    }
    
    // 删除回复方法
    public void removeReply(Comment reply) {
        replies.remove(reply);
        reply.setParent(null);
    }
} 