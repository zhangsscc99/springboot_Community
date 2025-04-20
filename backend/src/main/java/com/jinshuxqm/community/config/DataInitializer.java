package com.jinshuxqm.community.config;

import com.jinshuxqm.community.model.ERole;
import com.jinshuxqm.community.model.Role;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.repository.RoleRepository;
import com.jinshuxqm.community.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Profile("!test") // 非测试环境下执行
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 清理可能存在的重复角色
        cleanupDuplicateRoles();
        
        // 检查角色是否已存在
        if (roleRepository.count() == 0) {
            System.out.println("初始化角色数据...");
            initRoles();
        } else {
            System.out.println("角色数据已存在，跳过初始化");
        }
        
        // 检查管理员用户是否已存在
        if (!userRepository.existsByUsername("admin")) {
            System.out.println("初始化管理员用户...");
            initAdminUser();
        } else {
            System.out.println("管理员用户已存在，跳过初始化");
        }
    }

    /**
     * 清理数据库中存在的重复角色
     */
    private void cleanupDuplicateRoles() {
        // 检查ROLE_USER是否有重复
        List<Role> userRoles = roleRepository.findAllByName(ERole.ROLE_USER);
        if (userRoles.size() > 1) {
            System.out.println("发现重复的ROLE_USER角色，进行清理...");
            // 保留第一个角色，删除其他角色
            Role keepRole = userRoles.get(0);
            for (int i = 1; i < userRoles.size(); i++) {
                Role duplicateRole = userRoles.get(i);
                System.out.println("删除重复角色: " + duplicateRole.getId());
                roleRepository.delete(duplicateRole);
            }
        }
        
        // 检查ROLE_MODERATOR是否有重复
        List<Role> modRoles = roleRepository.findAllByName(ERole.ROLE_MODERATOR);
        if (modRoles.size() > 1) {
            System.out.println("发现重复的ROLE_MODERATOR角色，进行清理...");
            Role keepRole = modRoles.get(0);
            for (int i = 1; i < modRoles.size(); i++) {
                Role duplicateRole = modRoles.get(i);
                System.out.println("删除重复角色: " + duplicateRole.getId());
                roleRepository.delete(duplicateRole);
            }
        }
        
        // 检查ROLE_ADMIN是否有重复
        List<Role> adminRoles = roleRepository.findAllByName(ERole.ROLE_ADMIN);
        if (adminRoles.size() > 1) {
            System.out.println("发现重复的ROLE_ADMIN角色，进行清理...");
            Role keepRole = adminRoles.get(0);
            for (int i = 1; i < adminRoles.size(); i++) {
                Role duplicateRole = adminRoles.get(i);
                System.out.println("删除重复角色: " + duplicateRole.getId());
                roleRepository.delete(duplicateRole);
            }
        }
    }

    private void initRoles() {
        // 检查每种角色是否已存在，避免重复
        if (roleRepository.findAllByName(ERole.ROLE_USER).isEmpty()) {
            Role userRole = new Role();
            userRole.setName(ERole.ROLE_USER);
            roleRepository.save(userRole);
            System.out.println("已创建USER角色");
        }

        if (roleRepository.findAllByName(ERole.ROLE_MODERATOR).isEmpty()) {
            Role modRole = new Role();
            modRole.setName(ERole.ROLE_MODERATOR);
            roleRepository.save(modRole);
            System.out.println("已创建MODERATOR角色");
        }

        if (roleRepository.findAllByName(ERole.ROLE_ADMIN).isEmpty()) {
            Role adminRole = new Role();
            adminRole.setName(ERole.ROLE_ADMIN);
            roleRepository.save(adminRole);
            System.out.println("已创建ADMIN角色");
        }
        
        System.out.println("角色初始化成功！");
    }

    private void initAdminUser() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@jinshuxqm.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setAvatar("https://i.pinimg.com/474x/f1/9f/f7/f19ff7b351b4a4ca268b98e20fea7976.jpg");
        admin.setBio("锦书情感社区管理员");
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());

        Set<Role> roles = new HashSet<>();
        roleRepository.findByName(ERole.ROLE_ADMIN).ifPresent(roles::add);
        admin.setRoles(roles);

        userRepository.save(admin);
        
        System.out.println("管理员用户初始化成功！");
    }
} 