package com.jinshuxqm.community.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String avatar;
    private List<String> roles;

    public JwtResponse(String accessToken, Long id, String username, String email, String avatar, List<String> roles) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.avatar = avatar;
        this.roles = roles;
    }
} 