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
import org.springframework.security.core.Authentication;
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
    public PagedResponseDTO<CommentDTO> getCommentsByPostId(Long postId, int page, int size, Authentication authentication) {
        // 从Authentication中提取User
        User currentUser = null;
        if (authentication != null) {
            String username = authentication.getName();
            currentUser = userRepository.findByUsername(username).orElse(null);
        }
        
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
    public CommentDTO createComment(Long postId, CommentDTO commentDTO, Authentication authentication) {
        try {
            // 从Authentication中提取User
            User author = null;
            if (authentication != null) {
                String username = authentication.getName();
                author = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
            }
            
            Comment comment = new Comment();
            comment.setContent(commentDTO.getContent());
            
            // 处理null用户情况
            if (author == null) {
                // 获取系统用户
                author = userRepository.findByUsername("system")
                    .orElseThrow(() -> new RuntimeException("系统用户不存在"));
            }
            
            // 设置作者和帖子
            comment.setAuthor(author);
            
            Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
            comment.setPost(post);
            
            // 处理父评论(如果是回复)
            if (commentDTO.getParentId() != null) {
                Comment parentComment = commentRepository.findById(commentDTO.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentDTO.getParentId()));
                comment.setParent(parentComment);
            }
            
            // 保存评论
            comment = commentRepository.save(comment);
            
            // 将实体转换为DTO并返回
            return convertToDTO(comment, author);
        } catch (Exception e) {
            System.err.println("创建评论出错: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * 删除评论
     */
    @Override
    @Transactional
    public void deleteComment(Long commentId, Authentication authentication) {
        // 提取用户
        User currentUser = null;
        if (authentication != null) {
            currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("用户不存在"));
        } else {
            throw new UnauthorizedException("需要登录才能删除评论");
        }
        
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
    public void likeComment(Long commentId, Authentication authentication) {
        // 提取用户
        User currentUser = null;
        if (authentication != null) {
            currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("用户不存在"));
        } else {
            throw new UnauthorizedException("需要登录才能点赞评论");
        }
        
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
    public void unlikeComment(Long commentId, Authentication authentication) {
        // 提取用户
        User currentUser = null;
        if (authentication != null) {
            currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("用户不存在"));
        } else {
            throw new UnauthorizedException("需要登录才能取消点赞评论");
        }
        
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
        
        if (comment.isLikedByUser(currentUser)) {
            comment.removeLike(currentUser);
            commentRepository.save(comment);
        }
    }

    /**
     * 将评论实体转换为DTO
     */
    private CommentDTO convertToDTO(Comment comment, User currentUser) {
        // 使用构造函数或工厂方法创建DTO
        CommentDTO dto = CommentDTO.fromEntity(comment, false);
        
        // 只调用已知存在的setter
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setPostId(comment.getPost().getId());
        
        // 设置用户是否点赞
        if (currentUser != null) {
            dto.setLikedByCurrentUser(comment.isLikedByUser(currentUser));
        }
        
        if (comment.getParent() != null) {
            dto.setParentId(comment.getParent().getId());
        }
        
        // 处理回复
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            List<CommentDTO> replies = comment.getReplies().stream()
                .filter(reply -> !reply.getIsDeleted())
                .map(reply -> CommentDTO.fromEntity(reply, false))
                .collect(Collectors.toList());
            dto.setReplies(replies);
        }
        
        return dto;
    }

    @Override
    public List<CommentDTO> getCommentsByPostId(Long postId) {
        // 使用已有方法，返回简化的结果
        PagedResponseDTO<CommentDTO> pagedResponse = getCommentsByPostId(postId, 0, 20, null);
        return pagedResponse.getContent();
    }
} 