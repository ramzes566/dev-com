package com.y1.community.user.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.y1.community.user.entity.User;

import java.time.LocalDateTime;

public record UserVO(
        @JsonSerialize(using = ToStringSerializer.class)
        Long id,
        String username,
        String nickname,
        String avatar,
        String bio,
        Integer status,
        Integer followerCount,
        Integer followingCount,
        Integer postCount,
        LocalDateTime createdAt
) {
    public static UserVO from(User u) {
        return new UserVO(
                u.getId(),
                u.getUsername(),
                u.getNickname(),
                u.getAvatar(),
                u.getBio(),
                u.getStatus(),
                u.getFollowerCount(),
                u.getFollowingCount(),
                u.getPostCount(),
                u.getCreatedAt()
        );
    }
}