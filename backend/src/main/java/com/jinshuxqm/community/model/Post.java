package com.jinshuxqm.community.model;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User author;

    private String tab;
    
    @ElementCollection
    @CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private PostStats stats;
    
    // 添加点赞关联集合
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PostLike> likes = new HashSet<>();
    
    // 添加收藏关联集合
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PostFavorite> favorites = new HashSet<>();

    @Column(nullable = false)
    private Integer comments = 0;

    // 构造函数
    public Post() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getTab() {
        return tab;
    }

    public void setTab(String tab) {
        this.tab = tab;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public PostStats getStats() {
        return stats;
    }

    public void setStats(PostStats stats) {
        this.stats = stats;
    }
    
    // 新增 getter 和 setter
    public Set<PostLike> getLikes() {
        return likes;
    }
    
    public void setLikes(Set<PostLike> likes) {
        this.likes = likes;
    }
    
    public Set<PostFavorite> getFavorites() {
        return favorites;
    }
    
    public void setFavorites(Set<PostFavorite> favorites) {
        this.favorites = favorites;
    }
    
    // 帮助方法：添加一个点赞
    public void addLike(PostLike like) {
        likes.add(like);
        like.setPost(this);
    }
    
    // 帮助方法：移除一个点赞
    public void removeLike(PostLike like) {
        likes.remove(like);
        like.setPost(null);
    }
    
    // 帮助方法：添加一个收藏
    public void addFavorite(PostFavorite favorite) {
        favorites.add(favorite);
        favorite.setPost(this);
    }
    
    // 帮助方法：移除一个收藏
    public void removeFavorite(PostFavorite favorite) {
        favorites.remove(favorite);
        favorite.setPost(null);
    }

    public Integer getComments() {
        return comments;
    }

    public void setComments(Integer comments) {
        this.comments = comments;
    }
} 