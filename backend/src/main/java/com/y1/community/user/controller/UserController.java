package com.y1.community.user.controller;

import com.y1.community.user.service.UserService;
import com.y1.community.user.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserVO> list() {
        return userService.list()
                .stream()
                .map(UserVO::from)
                .toList();
    }
}