package com.manqiYang.hotelSystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**") // 允许所有路径
//                .allowedOrigins("*") // 允许所有来源
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的HTTP方法
//                .allowedHeaders("*") // 允许所有请求头
//                .allowCredentials(false) // 不允许携带凭证（如Cookies）
//                .maxAge(3600); // 预检请求的缓存时间（秒）
//    }
}