package com.jinshuxqm.community.controller;

import com.jinshuxqm.community.model.ERole;
import com.jinshuxqm.community.model.Role;
import com.jinshuxqm.community.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @Autowired
    private RoleRepository roleRepository;
    
    @GetMapping("/all")
    public String allAccess() {
        return "公共内容";
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public String userAccess() {
        return "用户内容";
    }

    @GetMapping("/mod")
    @PreAuthorize("hasRole('MODERATOR')")
    public String moderatorAccess() {
        return "管理员内容";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        return "超级管理员内容";
    }

    @GetMapping("/roles")
    public String checkRoles() {
        StringBuilder result = new StringBuilder();
        result.append("角色诊断信息:\n\n");
        
        try {
            // 获取所有角色
            List<Role> allRoles = roleRepository.findAll();
            result.append("所有角色数量: ").append(allRoles.size()).append("\n\n");
            
            for (Role role : allRoles) {
                result.append("ID: ").append(role.getId())
                      .append(", 名称: ").append(role.getName())
                      .append("\n");
            }
            
            // 检查是否有重复角色
            result.append("\n角色重复检测:\n");
            
            List<Role> userRoles = roleRepository.findAllByName(ERole.ROLE_USER);
            result.append("ROLE_USER: ").append(userRoles.size()).append(" 条记录\n");
            
            List<Role> modRoles = roleRepository.findAllByName(ERole.ROLE_MODERATOR);
            result.append("ROLE_MODERATOR: ").append(modRoles.size()).append(" 条记录\n");
            
            List<Role> adminRoles = roleRepository.findAllByName(ERole.ROLE_ADMIN);
            result.append("ROLE_ADMIN: ").append(adminRoles.size()).append(" 条记录\n");
            
            return result.toString();
        } catch (Exception e) {
            result.append("发生错误: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
            return result.toString();
        }
    }
}