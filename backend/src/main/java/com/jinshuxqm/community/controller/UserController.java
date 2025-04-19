package com.jinshuxqm.community.controller;

import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    UserRepository userRepository;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", user.getId());
                    response.put("username", user.getUsername());
                    response.put("email", user.getEmail());
                    response.put("avatar", user.getAvatar());
                    response.put("bio", user.getBio());
                    response.put("createdAt", user.getCreatedAt());
                    
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
} 