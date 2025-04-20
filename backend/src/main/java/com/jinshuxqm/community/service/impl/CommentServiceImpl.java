package com.jinshuxqm.community.service.impl;

import com.jinshuxqm.community.dto.CommentDTO;
import com.jinshuxqm.community.dto.PagedResponseDTO;
import com.jinshuxqm.community.exception.ResourceNotFoundException;
import com.jinshuxqm.community.exception.UnauthorizedException;
import com.jinshuxqm.community.model.Comment;
import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.repository.CommentRepository;
import com.jinshuxqm.community.repository.PostRepository;
import com.jinshuxqm.community.repository.UserRepository;
import com.jinshuxqm.community.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 获取帖子的评论（分页，带回复）
     */
    @Override
    public PagedResponseDTO<CommentDTO> getCommentsByPostId(Long postId, int page, int size, User currentUser) {
        Pageable pageable = PageRequest.of(page, size);
        
        // 验证帖子是否存在
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }
        
        // 获取一级评论
        Page<Comment> commentPage = commentRepository.findByPostIdAndParentIsNullOrderByCreatedAtDesc(postId, pageable);
        
        List<CommentDTO> commentDTOs = new ArrayList<>();
        
        for (Comment comment : commentPage.getContent()) {
            CommentDTO dto = CommentDTO.fromEntity(comment, true); // 带回复
            
            // 设置当前用户是否点赞
            if (currentUser != null) {
                dto.setLikedByCurrentUser(comment.isLikedByUser(currentUser));
            }
            
            commentDTOs.add(dto);
        }
        
        return new PagedResponseDTO<>(
            commentDTOs,
            commentPage.getNumber(),
            commentPage.getSize(),
            commentPage.getTotalElements(),
            commentPage.getTotalPages(),
            commentPage.isLast()
        );
    }
    
    /**
     * 创建评论
     */
    @Override
    @Transactional
    public CommentDTO createComment(Long postId, CommentDTO commentDTO, User currentUser) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        
        Comment comment = Comment.builder()
            .content(commentDTO.getContent())
            .post(post)
            .author(currentUser)
            .likes(0)
            .isDeleted(false)
            .build();
        
        // 如果是回复他人评论
        if (commentDTO.getParentId() != null) {
            Comment parentComment = commentRepository.findById(commentDTO.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentDTO.getParentId()));
            
            // 确保父评论属于同一帖子
            if (!parentComment.getPost().getId().equals(postId)) {
                throw new ResourceNotFoundException("Comment", "id", commentDTO.getParentId());
            }
            
            comment.setParent(parentComment);
            
            // 如果指定了回复对象
            if (commentDTO.getReplyToUser() != null && commentDTO.getReplyToUser().getId() != null) {
                User replyToUser = userRepository.findById(commentDTO.getReplyToUser().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", commentDTO.getReplyToUser().getId()));
                
                comment.setReplyToUser(replyToUser);
            } else {
                // 默认回复父评论的作者
                comment.setReplyToUser(parentComment.getAuthor());
            }
        }
        
        Comment savedComment = commentRepository.save(comment);
        
        // 更新帖子评论数
        long commentCount = commentRepository.countByPostIdAndIsDeletedFalse(postId);
        post.setComments((int) commentCount);
        postRepository.save(post);
        
        // 构建带有点赞信息的DTO
        CommentDTO savedCommentDTO = CommentDTO.fromEntity(savedComment, false);
        savedCommentDTO.setLikedByCurrentUser(false);
        
        return savedCommentDTO;
    }
    
    /**
     * 删除评论
     */
    @Override
    @Transactional
    public void deleteComment(Long commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
        
        // 检查权限：只有评论作者或帖子作者可以删除评论
        if (!comment.getAuthor().getId().equals(currentUser.getId()) &&
            !comment.getPost().getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't have permission to delete this comment");
        }
        
        // 软删除评论
        comment.setIsDeleted(true);
        commentRepository.save(comment);
        
        // 更新帖子评论数
        Post post = comment.getPost();
        long commentCount = commentRepository.countByPostIdAndIsDeletedFalse(post.getId());
        post.setComments((int) commentCount);
        postRepository.save(post);
    }
    
    /**
     * 点赞评论
     */
    @Override
    @Transactional
    public void likeComment(Long commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
        
        if (!comment.isLikedByUser(currentUser)) {
            comment.addLike(currentUser);
            commentRepository.save(comment);
        }
    }
    
    /**
     * 取消点赞评论
     */
    @Override
    @Transactional
    public void unlikeComment(Long commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
        
        if (comment.isLikedByUser(currentUser)) {
            comment.removeLike(currentUser);
            commentRepository.save(comment);
        }
    }
} 