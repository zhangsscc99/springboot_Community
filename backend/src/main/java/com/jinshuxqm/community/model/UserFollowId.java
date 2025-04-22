package com.jinshuxqm.community.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFollowId implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Column(name = "follower_id")
    private Long followerId;
    
    @Column(name = "followee_id")
    private Long followeeId;
} 