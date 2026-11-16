package com.y1.community.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password) {

    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";

    }
}