package com.jinshuxqm.community.dto;

import com.jinshuxqm.community.model.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    
    private Long id;
    private String content;
    private Long postId;
    private UserDTO author;
    private Integer likes;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private Boolean likedByCurrentUser;
    private Long parentId;
    private UserDTO replyToUser;
    private List<CommentDTO> replies;
    
    public static CommentDTO fromEntity(Comment comment, boolean withReplies) {
        if (comment == null) return null;
        
        CommentDTO dto = CommentDTO.builder()
            .id(comment.getId())
            .content(comment.getContent())
            .postId(comment.getPost().getId())
            .author(UserDTO.fromEntity(comment.getAuthor()))
            .likes(comment.getLikes())
            .created_at(comment.getCreatedAt())
            .updated_at(comment.getUpdatedAt())
            .build();
        
        if (comment.getParent() != null) {
            dto.setParentId(comment.getParent().getId());
        }
        
        if (comment.getReplyToUser() != null) {
            dto.setReplyToUser(UserDTO.fromEntity(comment.getReplyToUser()));
        }
        
        // 递归构建回复
        if (withReplies && comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            List<CommentDTO> replyDTOs = comment.getReplies().stream()
                .filter(reply -> !reply.getIsDeleted())
                .map(reply -> CommentDTO.fromEntity(reply, false)) // 避免无限递归
                .collect(Collectors.toList());
            dto.setReplies(replyDTOs);
        }
        
        return dto;
    }
} 