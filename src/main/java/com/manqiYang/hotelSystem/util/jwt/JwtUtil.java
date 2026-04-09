package com.manqiYang.hotelSystem.util.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET = "ReplaceWithStrongSecretKey_AtLeast32Chars!";
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET.getBytes());
    private static final long EXPIRE_TIME = 1000L * 60 * 60 * 24; // 24小时

    // 生成 Token
    public static String createToken(Long id, String phone, String role) {
        return Jwts.builder()
                .setSubject(String.valueOf(id))
                .claim("phone", phone)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(KEY)
                .compact();
    }

    // 解析 Token
    public static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Token已过期");
        } catch (UnsupportedJwtException e) {
            throw new RuntimeException("Token格式错误");
        } catch (MalformedJwtException e) {
            throw new RuntimeException("Token非法");
        } catch (SignatureException e) {
            throw new RuntimeException("签名错误");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Token为空");
        }
    }

    // 获取用户ID
    public static Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    // 获取手机号
    public static String getPhone(String token) {
        return parseToken(token).get("phone", String.class);
    }

    // 获取角色
    public static String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }
}