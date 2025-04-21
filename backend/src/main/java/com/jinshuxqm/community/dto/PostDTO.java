package com.jinshuxqm.community.dto;

import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private String slug;
    private UserDTO author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int likes;
    private int comments;
    private int views;
    private int favorites;
    private List<String> tags;
    private boolean likedByCurrentUser;
    private boolean favoritedByCurrentUser;
    
    // 静态工厂方法，将Post实体转换为DTO
    public static PostDTO fromEntity(Post post, User currentUser) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setSlug(post.getSlug());
        dto.setAuthor(UserDTO.fromEntity(post.getAuthor()));
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        dto.setLikes(post.getLikes().size());
        dto.setComments(post.getComments());
        dto.setViews(post.getViews());
        dto.setFavorites(post.getFavorites().size());
        
        // 设置标签
        if (post.getTags() != null) {
            dto.setTags(new ArrayList<>(post.getTags()));
        }
        
        // 设置当前用户是否点赞/收藏
        if (currentUser != null) {
            dto.setLikedByCurrentUser(post.getLikes().contains(currentUser));
            dto.setFavoritedByCurrentUser(post.getFavorites().contains(currentUser));
        }
        
        return dto;
    }
} 