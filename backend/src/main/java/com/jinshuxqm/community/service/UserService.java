package com.jinshuxqm.community.service;

import com.jinshuxqm.community.model.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

public interface UserService {
    User findById(Long id);
    User findByUsername(String username);
    User getCurrentUser();
} 