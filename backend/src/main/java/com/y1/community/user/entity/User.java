package com.y1.community.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String username;

    @JsonIgnore
    private String passwordHash;

    private String nickname;
    private String avatar;
    private String bio;

    private Integer status;

    private Integer followerCount;
    private Integer followingCount;
    private Integer postCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}