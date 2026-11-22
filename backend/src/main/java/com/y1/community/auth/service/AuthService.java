package com.y1.community.auth.service;

import com.y1.community.auth.dto.LoginRequest;
import com.y1.community.auth.dto.TokenResponse;
import com.y1.community.security.JwtService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.y1.community.auth.dto.RegisterRequest;
import com.y1.community.user.entity.User;
import com.y1.community.user.mapper.UserMapper;
import com.y1.community.user.vo.UserVO;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public UserVO register(RegisterRequest req) {
        boolean taken = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, req.username())) > 0;
        if (taken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已被占用");
        }

        User user = new User();
        user.setUsername(req.username());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setNickname(StringUtils.hasText(req.nickname()) ? req.nickname() : req.username());
        user.setAvatar("");
        user.setBio("");
        user.setStatus(0);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已被占用");
        }

        return UserVO.from(userMapper.selectById(user.getId()));
    }

    public TokenResponse login(LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        } catch (DisabledException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已被封禁");
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, req.username()));

        String accessToken = jwtService.createAccessToken(user);
        return TokenResponse.bearer(accessToken,
                jwtService.accessTtlSeconds());

    }

}