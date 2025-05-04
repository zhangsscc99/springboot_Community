package com.jinshuxqm.community.dto;

import com.jinshuxqm.community.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String bio;
    
    public static UserDTO fromEntity(User user) {
        if (user == null) return null;
        
        return UserDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .avatar(user.getAvatar())
            .email(user.getEmail())
            .bio(user.getBio())
            .build();
    }
} 