package com.y1.community.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.y1.community.auth.dto.LoginRequest;
import com.y1.community.auth.dto.RegisterRequest;
import com.y1.community.auth.dto.TokenResponse;
import com.y1.community.security.JwtService;
import com.y1.community.security.RefreshTokenStore;
import com.y1.community.user.entity.User;
import com.y1.community.user.mapper.UserMapper;
import com.y1.community.user.vo.UserVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;

    public AuthService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService, RefreshTokenStore refreshTokenStore) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
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

        return issueTokens(user);

    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.createAccessToken(user);

        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtService.createRefreshToken(user, jti);
        refreshTokenStore.save(jti, user.getId(), jwtService.refreshTtl());

        return TokenResponse.bearer(accessToken, refreshToken, jwtService.accessTtlSeconds());
    }

    public TokenResponse refresh(String refreshToken) {
        Claims claims = parseRefresh(refreshToken);
        String jti = claims.getId();
        Long userId = Long.valueOf(claims.getSubject());

        if(!refreshTokenStore.consume(jti)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh token 已失效");
        }

        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(2).equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号不可用");
        }
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        try {
            refreshTokenStore.revoke(parseRefresh(refreshToken).getId());
        } catch (ResponseStatusException ignored) {}
    }

    private Claims parseRefresh(String token) {
        try {
            Claims claims = jwtService.parse(token);
            if (!"refresh".equals(claims.get("typ", String.class))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "token 类型不对");
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh token 无效");
        }
    }

}