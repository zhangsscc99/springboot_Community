package com.jinshuxqm.community.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "username"),
           @UniqueConstraint(columnNames = "email")
       })
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 20)
    private String username;

    @Email(message = "邮箱格式不正确")
    @Size(max = 50)
    private String email;

    @NotBlank
    @Size(max = 120)
    private String password;

    @Size(max = 255)
    private String avatar;

    @Size(max = 200)
    private String bio;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
               joinColumns = @JoinColumn(name = "user_id"),
               inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
    
    // 添加用户点赞集合
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PostLike> likes = new HashSet<>();
    
    // 添加用户收藏集合
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PostFavorite> favorites = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // 帮助方法：添加一个点赞
    public void addLike(PostLike like) {
        likes.add(like);
        like.setUser(this);
    }
    
    // 帮助方法：移除一个点赞
    public void removeLike(PostLike like) {
        likes.remove(like);
        like.setUser(null);
    }
    
    // 帮助方法：添加一个收藏
    public void addFavorite(PostFavorite favorite) {
        favorites.add(favorite);
        favorite.setUser(this);
    }
    
    // 帮助方法：移除一个收藏
    public void removeFavorite(PostFavorite favorite) {
        favorites.remove(favorite);
        favorite.setUser(null);
    }
} 