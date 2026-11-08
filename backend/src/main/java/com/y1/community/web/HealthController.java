package com.y1.community.web;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@RestController
public class HealthController {

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final ConnectionFactory rabbit;

    public HealthController(JdbcTemplate jdbc, StringRedisTemplate redis, ConnectionFactory rabbit) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.rabbit = rabbit;
    }

    @GetMapping("/api/Health")
    public Map<String, String> health() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("redis", probe(() -> redis.hasKey("__health__")));
        result.put("db", probe(() -> jdbc.queryForObject("SELECT 1", Integer.class)));
        result.put("mq", probe(() -> {
            var conn = rabbit.createConnection();
            try {
                return Boolean.TRUE;
            } finally {
                conn.close();
            }
        }));
        return result;
    }

    private String probe(Supplier<?> action) {
        try {
            action.get();
            return "ok";
        } catch (Exception e) {
            return "fail: " + e.getClass().getSimpleName();
        }
    }
}
