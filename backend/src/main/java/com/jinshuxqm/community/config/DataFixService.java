package com.jinshuxqm.community.config;

import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.repository.PostRepository;
import com.jinshuxqm.community.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 数据修复服务
 * 在应用启动时检测并修复Agent帖子作者问题
 */
@Component
@Profile("!test") // 非测试环境下执行
@Order(1) // 在PostDataInitializer之前执行
public class DataFixService implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataFixService.class);
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("🔧 开始检查数据完整性...");
        
        // 检查是否需要修复帖子作者问题
        if (needsPostAuthorFix()) {
            logger.info("🚨 检测到帖子作者数据异常，开始修复...");
            fixPostAuthorIssues();
        } else {
            logger.info("✅ 数据完整性检查通过，无需修复");
        }
    }
    
    /**
     * 检查是否需要修复帖子作者问题
     * 如果所有帖子都指向admin用户且存在agent用户，则需要修复
     */
    private boolean needsPostAuthorFix() {
        try {
            // 获取所有帖子
            List<Post> allPosts = postRepository.findAll();
            if (allPosts.isEmpty()) {
                return false; // 没有帖子，不需要修复
            }
            
            // 检查admin用户
            Optional<User> adminUser = userRepository.findByUsername("admin");
            if (!adminUser.isPresent()) {
                return false; // 没有admin用户，不是我们要解决的问题
            }
            
            // 检查是否有agent用户存在
            boolean hasAgentUsers = userRepository.findByUsername("city_girl").isPresent() ||
                                  userRepository.findByUsername("career_sister").isPresent() ||
                                  userRepository.findByUsername("teen_heart").isPresent() ||
                                  userRepository.findByUsername("family_man").isPresent() ||
                                  userRepository.findByUsername("lovelessboy").isPresent() ||
                                  userRepository.findByUsername("xiaoming").isPresent();
            
            if (!hasAgentUsers) {
                return false; // 没有agent用户，可能还没初始化
            }
            
            // 检查是否所有帖子都指向admin用户
            long adminPostCount = allPosts.stream()
                .filter(post -> post.getAuthor().getId().equals(adminUser.get().getId()))
                .count();
            
            // 如果80%以上的帖子都指向admin用户，认为需要修复
            boolean needsFix = adminPostCount >= (allPosts.size() * 0.8);
            
            if (needsFix) {
                logger.info("📊 检测到异常：共 {} 个帖子，其中 {} 个指向admin用户，需要修复", 
                    allPosts.size(), adminPostCount);
            }
            
            return needsFix;
        } catch (Exception e) {
            logger.error("❌ 检查数据完整性时出错", e);
            return false;
        }
    }
    
    /**
     * 修复帖子作者问题
     * 删除所有现有帖子，让PostDataInitializer重新创建正确的数据
     */
    @Transactional
    private void fixPostAuthorIssues() {
        try {
            logger.info("🗑️ 开始清理错误的帖子数据...");
            
            // 删除所有帖子相关数据
            long postCount = postRepository.count();
            
            // 执行删除操作
            postRepository.deleteAll();
            
            logger.info("✅ 已删除 {} 个帖子，数据将在下次初始化时重新创建", postCount);
            logger.info("🔄 请等待PostDataInitializer重新初始化示例帖子...");
            
        } catch (Exception e) {
            logger.error("❌ 修复帖子作者问题时出错", e);
        }
    }
} 