package com.manqiYang.hotelSystem.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import com.manqiYang.hotelSystem.util.jwt.JwtUtil;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.data.redis.core.RedisTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        
        String token = request.getHeader("Authorization");

        Boolean isBlacklisted = redisTemplate.hasKey("jwt_blacklist:" + token);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            throw new RuntimeException("Token已失效，请重新登录");
        }

        JwtUtil.parseToken(token);
        return true;
    }
}
