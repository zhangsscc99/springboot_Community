package com.jinshuxqm.community.service;

import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.model.UserFollow;
import com.jinshuxqm.community.model.UserFollowId;
import com.jinshuxqm.community.repository.UserFollowRepository;
import com.jinshuxqm.community.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserFollowService {

    @Autowired
    private UserFollowRepository userFollowRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Follow a user
     * @param followerId the ID of the follower
     * @param followeeId the ID of the user to follow
     * @return true if the follow operation was successful
     */
    @Transactional
    public boolean followUser(Long followerId, Long followeeId) {
        // Check if the IDs are the same
        if (followerId.equals(followeeId)) {
            return false; // Users cannot follow themselves
        }
        
        // Check if already following
        if (userFollowRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            return false; // Already following
        }
        
        // Get the users
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower user not found"));
        User followee = userRepository.findById(followeeId)
                .orElseThrow(() -> new RuntimeException("Followee user not found"));
        
        // Create the follow relationship
        UserFollowId id = new UserFollowId(followerId, followeeId);
        UserFollow userFollow = new UserFollow();
        userFollow.setId(id);
        userFollow.setFollower(follower);
        userFollow.setFollowee(followee);
        
        userFollowRepository.save(userFollow);
        return true;
    }
    
    /**
     * Unfollow a user
     * @param followerId the ID of the follower
     * @param followeeId the ID of the user to unfollow
     * @return true if the unfollow operation was successful
     */
    @Transactional
    public boolean unfollowUser(Long followerId, Long followeeId) {
        // Check if the follow relationship exists
        if (!userFollowRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            return false; // Not following
        }
        
        userFollowRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
        return true;
    }
    
    /**
     * Check if a user is following another user
     * @param followerId the ID of the potential follower
     * @param followeeId the ID of the potential followee
     * @return true if the follow relationship exists
     */
    public boolean isFollowing(Long followerId, Long followeeId) {
        return userFollowRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }
    
    /**
     * Get the followers of a user
     * @param userId the ID of the user
     * @return a list of users who follow the given user
     */
    public List<User> getFollowers(Long userId) {
        return userFollowRepository.findAllByFolloweeId(userId).stream()
                .map(UserFollow::getFollower)
                .collect(Collectors.toList());
    }
    
    /**
     * Get the users that a user follows
     * @param userId the ID of the user
     * @return a list of users that the given user follows
     */
    public List<User> getFollowing(Long userId) {
        return userFollowRepository.findAllByFollowerId(userId).stream()
                .map(UserFollow::getFollowee)
                .collect(Collectors.toList());
    }
    
    /**
     * Count the number of followers a user has
     * @param userId the ID of the user
     * @return the number of followers
     */
    public Long countFollowers(Long userId) {
        return userFollowRepository.countFollowersByUserId(userId);
    }
    
    /**
     * Count the number of users a user follows
     * @param userId the ID of the user
     * @return the number of users followed
     */
    public Long countFollowing(Long userId) {
        return userFollowRepository.countFollowingByUserId(userId);
    }
} 