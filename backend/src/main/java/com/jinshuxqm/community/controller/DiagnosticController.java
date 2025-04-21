package com.jinshuxqm.community.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/diagnostic")
public class DiagnosticController {
    
    @GetMapping("/security-status")
    public ResponseEntity<Map<String, Object>> getSecurityStatus(HttpServletRequest request) {
        Map<String, Object> status = new HashMap<>();
        
        // 获取认证信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        status.put("authenticated", auth != null && auth.isAuthenticated());
        if (auth != null) {
            status.put("principal", auth.getPrincipal().toString());
            status.put("authorities", auth.getAuthorities().toString());
        }
        
        // 收集请求头信息
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }
        status.put("headers", headers);
        
        return ResponseEntity.ok(status);
    }
    
    // 这个端点允许匿名访问，可以测试公开API是否正常工作
    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> publicEndpoint() {
        return ResponseEntity.ok(Collections.singletonMap("message", "这是一个公开端点，任何人都可以访问"));
    }
    
    // 这个端点需要认证，可以测试认证是否正常工作
    @GetMapping("/protected")
    public ResponseEntity<Map<String, String>> protectedEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(Collections.singletonMap("message", 
            "这是一个受保护的端点，用户 " + auth.getName() + " 已通过认证"));
    }
} 