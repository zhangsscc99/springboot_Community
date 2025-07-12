package com.jinshuxqm.community.agent.model;

import java.time.LocalTime;
import java.util.List;

/**
 * Agent配置基础类，用于存储每个Agent的个性化配置
 */
public class AgentConfig {
    private String username;          // 用户名
    private String password;          // 密码
    private String email;             // 邮箱
    private String nickname;          // 昵称
    private String bio;               // 个人简介
    private int age;                  // 年龄
    private List<String> interests;   // 兴趣爱好
    private double postProbability;   // 发帖概率 (0-1)
    private double likeProbability;   // 点赞概率 (0-1)
    private double commentProbability; // 评论概率 (0-1)
    private double followProbability; // 关注概率 (0-1)
    private double favoriteProbability; // 收藏概率 (0-1)
    private LocalTime activeStartTime; // 活跃开始时间
    private LocalTime activeEndTime;   // 活跃结束时间
    private List<String> postTitles;  // 可能发布的帖子标题
    private List<String> postContents; // 可能发布的帖子内容
    private List<String> comments;    // 可能发布的评论

    // 构造函数
    public AgentConfig() {
    }
    
    public AgentConfig(String username, String password, String email, String nickname, 
                     String bio, int age, List<String> interests,
                     double postProb, double likeProb, double commentProb, 
                     double followProb, double favoriteProb,
                     LocalTime activeStart, LocalTime activeEnd,
                     List<String> titles, List<String> contents, List<String> comments) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.nickname = nickname;
        this.bio = bio;
        this.age = age;
        this.interests = interests;
        this.postProbability = postProb;
        this.likeProbability = likeProb;
        this.commentProbability = commentProb;
        this.followProbability = followProb;
        this.favoriteProbability = favoriteProb;
        this.activeStartTime = activeStart;
        this.activeEndTime = activeEnd;
        this.postTitles = titles;
        this.postContents = contents;
        this.comments = comments;
    }
    
    // 检查当前时间是否在活跃时段
    public boolean isActiveNow() {
        LocalTime now = LocalTime.now();
        
        // 如果设置为全天活跃（00:00-23:59），直接返回true
        if (activeStartTime.equals(LocalTime.of(0, 0)) && 
            activeEndTime.equals(LocalTime.of(23, 59))) {
            return true;
        }
        
        // 使用包含边界的时间比较
        return (!now.isBefore(activeStartTime) && !now.isAfter(activeEndTime));
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    public double getPostProbability() {
        return postProbability;
    }

    public void setPostProbability(double postProbability) {
        this.postProbability = postProbability;
    }

    public double getLikeProbability() {
        return likeProbability;
    }

    public void setLikeProbability(double likeProbability) {
        this.likeProbability = likeProbability;
    }

    public double getCommentProbability() {
        return commentProbability;
    }

    public void setCommentProbability(double commentProbability) {
        this.commentProbability = commentProbability;
    }

    public double getFollowProbability() {
        return followProbability;
    }

    public void setFollowProbability(double followProbability) {
        this.followProbability = followProbability;
    }

    public double getFavoriteProbability() {
        return favoriteProbability;
    }

    public void setFavoriteProbability(double favoriteProbability) {
        this.favoriteProbability = favoriteProbability;
    }

    public LocalTime getActiveStartTime() {
        return activeStartTime;
    }

    public void setActiveStartTime(LocalTime activeStartTime) {
        this.activeStartTime = activeStartTime;
    }

    public LocalTime getActiveEndTime() {
        return activeEndTime;
    }

    public void setActiveEndTime(LocalTime activeEndTime) {
        this.activeEndTime = activeEndTime;
    }

    public List<String> getPostTitles() {
        return postTitles;
    }

    public void setPostTitles(List<String> postTitles) {
        this.postTitles = postTitles;
    }

    public List<String> getPostContents() {
        return postContents;
    }

    public void setPostContents(List<String> postContents) {
        this.postContents = postContents;
    }

    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
    }
} 