package com.jinshuxqm.community.model.dto;

import java.time.LocalDateTime;
import java.util.Set;

public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private UserSummary author;
    private String tab;
    private Set<String> tags;
    private int likes;
    private int comments;
    private int favorites;
    private int views;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 添加用户的点赞和收藏状态
    private boolean likedByCurrentUser;
    private boolean favoritedByCurrentUser;

    // 作者简要信息的内部类
    public static class UserSummary {
        private Long id;
        private String username;
        private String avatar;
        private String bio;

        public UserSummary(Long id, String username, String avatar) {
            this.id = id;
            this.username = username;
            this.avatar = avatar;
        }
        
        public UserSummary(Long id, String username, String avatar, String bio) {
            this.id = id;
            this.username = username;
            this.avatar = avatar;
            this.bio = bio;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }
        
        public String getBio() {
            return bio;
        }
        
        public void setBio(String bio) {
            this.bio = bio;
        }
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

    public UserSummary getAuthor() {
        return author;
    }

    public void setAuthor(UserSummary author) {
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

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getComments() {
        return comments;
    }

    public void setComments(int comments) {
        this.comments = comments;
    }

    public int getFavorites() {
        return favorites;
    }

    public void setFavorites(int favorites) {
        this.favorites = favorites;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
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
    
    // 新增的getter和setter
    public boolean isLikedByCurrentUser() {
        return likedByCurrentUser;
    }
    
    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        this.likedByCurrentUser = likedByCurrentUser;
    }
    
    public boolean isFavoritedByCurrentUser() {
        return favoritedByCurrentUser;
    }
    
    public void setFavoritedByCurrentUser(boolean favoritedByCurrentUser) {
        this.favoritedByCurrentUser = favoritedByCurrentUser;
    }
} 