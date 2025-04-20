package com.jinshuxqm.community.service.impl;

import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.repository.UserRepository;
import com.jinshuxqm.community.service.UserService;
import com.jinshuxqm.community.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    @Override
    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return findByUsername(username);
    }
} 