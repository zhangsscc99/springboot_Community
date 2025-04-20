package com.jinshuxqm.community.repository;

import com.jinshuxqm.community.model.ERole;
import com.jinshuxqm.community.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(ERole name);
    
    // 修复重复角色问题
    @Query("SELECT r FROM Role r WHERE r.name = ?1")
    List<Role> findAllByName(ERole name);

    // 通过名称查找第一个角色 - 使用方法命名约定
    Optional<Role> findTopByNameOrderByIdAsc(ERole name);
} 