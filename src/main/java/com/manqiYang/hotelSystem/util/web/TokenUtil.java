package com.manqiYang.hotelSystem.util.web;

public class TokenUtil {
    private static final String PREFIX = "Bearer ";

    public static String extractToken(String header) {

        if (header == null || !header.startsWith(PREFIX)) {
            throw new RuntimeException("Authorization格式错误");
        }

        return header.substring(PREFIX.length());
    }
}
