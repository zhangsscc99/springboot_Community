package com.jinshuxqm.community.service;

import com.jinshuxqm.community.dto.CommentDTO;
import com.jinshuxqm.community.dto.PagedResponseDTO;
import com.jinshuxqm.community.model.User;

public interface CommentService {
    PagedResponseDTO<CommentDTO> getCommentsByPostId(Long postId, int page, int size, User currentUser);
    CommentDTO createComment(Long postId, CommentDTO commentDTO, User currentUser);
    void deleteComment(Long commentId, User currentUser);
    void likeComment(Long commentId, User currentUser);
    void unlikeComment(Long commentId, User currentUser);
} 