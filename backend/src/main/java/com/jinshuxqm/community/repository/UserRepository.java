package com.jinshuxqm.community.repository;

import com.jinshuxqm.community.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByEmail(String email);
    
    // 通过用户名列表查找用户
    List<User> findByUsernameIn(List<String> usernames);
    
    // 通过用户名或昵称搜索用户
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> searchByUsernameOrNickname(@Param("query") String query);
} 