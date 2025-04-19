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
        // 检查角色是否已存在
        if (roleRepository.count() == 0) {
            System.out.println("初始化角色数据...");
            initRoles();
        }
        
        // 检查管理员用户是否已存在
        if (!userRepository.existsByUsername("admin")) {
            System.out.println("初始化管理员用户...");
            initAdminUser();
        }
    }

    private void initRoles() {
        Role userRole = new Role();
        userRole.setName(ERole.ROLE_USER);
        roleRepository.save(userRole);

        Role modRole = new Role();
        modRole.setName(ERole.ROLE_MODERATOR);
        roleRepository.save(modRole);

        Role adminRole = new Role();
        adminRole.setName(ERole.ROLE_ADMIN);
        roleRepository.save(adminRole);
        
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