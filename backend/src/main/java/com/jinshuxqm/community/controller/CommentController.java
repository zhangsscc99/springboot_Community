package com.jinshuxqm.community.controller;

import com.jinshuxqm.community.dto.CommentDTO;
import com.jinshuxqm.community.dto.PagedResponseDTO;
import com.jinshuxqm.community.exception.UnauthorizedException;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.service.CommentService;
import com.jinshuxqm.community.service.PostService;
import com.jinshuxqm.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    // 获取帖子的评论
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<PagedResponseDTO<CommentDTO>> getCommentsByPostId(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser) {
        
        try {
            // 添加调试日志
            System.out.println("接收到评论获取请求: 帖子ID=" + postId + ", 页码=" + page + ", 大小=" + size);
            
            // 检查帖子是否存在
            if (!postService.existsById(postId)) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(commentService.getCommentsByPostId(postId, page, size, currentUser));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 添加评论
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentDTO> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentDTO commentDTO,
            @AuthenticationPrincipal User currentUser) {
        
        if (currentUser == null) {
            throw new UnauthorizedException("用户未登录");
        }
        
        try {
            // 设置帖子ID
            commentDTO.setPostId(postId);
            
            // 记录接收到的请求
            System.out.println("接收到评论请求: " + commentDTO);
            
            // 创建评论
            CommentDTO createdComment = commentService.createComment(postId, commentDTO, currentUser);
            
            // 返回创建的评论
            return ResponseEntity.ok(createdComment);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    // 添加回复
    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<CommentDTO> addReply(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentDTO replyDTO,
            @AuthenticationPrincipal User currentUser) {
        
        return ResponseEntity.ok(commentService.createComment(replyDTO.getPostId(), replyDTO, currentUser));
    }

    // 删除评论
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal User currentUser) {
        
        commentService.deleteComment(commentId, currentUser);
        return ResponseEntity.ok().build();
    }

    // 点赞评论
    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<Void> likeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal User currentUser) {
        
        commentService.likeComment(commentId, currentUser);
        return ResponseEntity.ok().build();
    }

    // 取消点赞评论
    @DeleteMapping("/comments/{commentId}/like")
    public ResponseEntity<Void> unlikeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal User currentUser) {
        
        commentService.unlikeComment(commentId, currentUser);
        return ResponseEntity.ok().build();
    }
} 