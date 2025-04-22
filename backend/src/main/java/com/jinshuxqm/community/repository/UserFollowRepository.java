package com.jinshuxqm.community.repository;

import com.jinshuxqm.community.model.UserFollow;
import com.jinshuxqm.community.model.UserFollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, UserFollowId> {
    
    // Find all users that the current user follows
    List<UserFollow> findAllByFollowerId(Long followerId);
    
    // Find all users that follow the current user
    List<UserFollow> findAllByFolloweeId(Long followeeId);
    
    // Check if a follow relationship exists
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    
    // Count followers
    @Query("SELECT COUNT(uf) FROM UserFollow uf WHERE uf.followee.id = :userId")
    Long countFollowersByUserId(Long userId);
    
    // Count following
    @Query("SELECT COUNT(uf) FROM UserFollow uf WHERE uf.follower.id = :userId")
    Long countFollowingByUserId(Long userId);
    
    // Delete a follow relationship
    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
} 