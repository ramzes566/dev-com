package com.y1.community;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.y1.community.security.JwtService;
import com.y1.community.user.entity.User;
import com.y1.community.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class UserApiTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtService jwtService;

    @Autowired
    UserMapper userMapper;

    private String demoToken() {
        User demo = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, "demo")
        );
        return jwtService.createAccessToken(demo);
    }

    @Test
    void seedUserCanBeQueried() throws Exception {
        mvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + demoToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", hasItem("demo")));
    }

    @Test
    void responseMustNotLeakPassword() throws Exception {
        mvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + demoToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(content().string(not(containsString("$2a$"))));
    }

}
