package com.jinshuxqm.community.repository;

import com.jinshuxqm.community.model.ERole;
import com.jinshuxqm.community.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(ERole name);
} 