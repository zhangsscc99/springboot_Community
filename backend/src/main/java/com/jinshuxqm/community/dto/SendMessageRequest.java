package com.jinshuxqm.community.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    @NotNull(message = "接收者ID不能为空")
    private Long receiverId;
    
    @NotBlank(message = "消息内容不能为空")
    private String content;
} 