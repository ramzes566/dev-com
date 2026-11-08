package com.y1.community.user.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.y1.community.user.entity.User;
import com.y1.community.user.mapper.UserMapper;
import com.y1.community.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}