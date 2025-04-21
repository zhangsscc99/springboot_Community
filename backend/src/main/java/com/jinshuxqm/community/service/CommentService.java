package com.jinshuxqm.community.service;

import com.jinshuxqm.community.dto.CommentDTO;
import com.jinshuxqm.community.dto.PagedResponseDTO;
import com.jinshuxqm.community.model.User;
import org.springframework.security.core.Authentication;
import java.util.List;

public interface CommentService {
    PagedResponseDTO<CommentDTO> getCommentsByPostId(Long postId, int page, int size, Authentication authentication);
    CommentDTO createComment(Long postId, CommentDTO commentDTO, Authentication authentication);
    void deleteComment(Long commentId, Authentication authentication);
    void likeComment(Long commentId, Authentication authentication);
    void unlikeComment(Long commentId, Authentication authentication);
    List<CommentDTO> getCommentsByPostId(Long postId);
} 