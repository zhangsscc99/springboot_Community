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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/")
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
            Authentication authentication) {
        
        try {
            // 添加调试日志
            System.out.println("接收到评论获取请求: 帖子ID=" + postId + ", 页码=" + page + ", 大小=" + size);
            
            // 检查帖子是否存在
            if (!postService.existsById(postId)) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(commentService.getCommentsByPostId(postId, page, size, authentication));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 添加评论
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentDTO commentDTO,
            Authentication authentication) {
        
        System.out.println("=== 收到评论创建请求 ===");
        System.out.println("路径参数 postId: " + postId);
        System.out.println("请求体: " + commentDTO);
        
        try {
            // 从Authentication直接获取用户
            User currentUser = null;
            if (authentication != null) {
                String username = authentication.getName();
                currentUser = userService.findByUsername(username);
                System.out.println("认证名称: " + username);
                System.out.println("当前用户: " + (currentUser != null ? currentUser.getUsername() : "未找到用户"));
            } else {
                System.out.println("认证对象为null");
            }
            
            // 设置帖子ID和详细日志
            commentDTO.setPostId(postId);
            System.out.println("接收到评论请求: postId=" + postId + ", 用户=" + 
                (currentUser != null ? currentUser.getUsername() : "系统用户") + 
                ", 内容=" + commentDTO.getContent());
            
            // 检查帖子是否存在
            if (!postService.existsById(postId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("要评论的帖子不存在");
            }
            
            // 创建评论
            CommentDTO createdComment = commentService.createComment(postId, commentDTO, authentication);
            System.out.println("评论创建成功，ID: " + createdComment.getId());
            
            // 返回创建的评论
            return ResponseEntity.ok(createdComment);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("创建评论时发生错误: " + e.getMessage());
        }
    }

    // 添加回复
    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<CommentDTO> addReply(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentDTO replyDTO,
            Authentication authentication) {
        
        return ResponseEntity.ok(commentService.createComment(replyDTO.getPostId(), replyDTO, authentication));
    }

    // 删除评论
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        
        commentService.deleteComment(commentId, authentication);
        return ResponseEntity.ok().build();
    }

    // 点赞评论
    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<Void> likeComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        
        commentService.likeComment(commentId, authentication);
        return ResponseEntity.ok().build();
    }

    // 取消点赞评论
    @DeleteMapping("/comments/{commentId}/like")
    public ResponseEntity<Void> unlikeComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        
        commentService.unlikeComment(commentId, authentication);
        return ResponseEntity.ok().build();
    }
} 