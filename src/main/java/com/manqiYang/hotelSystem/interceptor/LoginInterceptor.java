package com.manqiYang.hotelSystem.interceptor;

import com.manqiYang.hotelSystem.context.UserContext;
import com.manqiYang.hotelSystem.entity.user.SysUser;
import com.manqiYang.hotelSystem.enums.user.UserStatusEnum;
import com.manqiYang.hotelSystem.mapper.user.UserMapper;
import com.manqiYang.hotelSystem.model.UserInfo;
import com.manqiYang.hotelSystem.util.jwt.JwtUtil;
import com.manqiYang.hotelSystem.util.web.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        String header = request.getHeader("Authorization");

        String token = TokenUtil.extractToken(header);

        Long userId = JwtUtil.getUserId(token);

        SysUser sysUser = userMapper.selectById(userId);
        if (sysUser == null) {
            throw new RuntimeException("用户不存在或token无效");
        }

        UserInfo user = new UserInfo();
        user.setUserId(userId);
        user.setHotelId(sysUser.getHotelId());
        user.setUsername(sysUser.getUsername());
        user.setStatus(sysUser.getStatus() == 1 ? UserStatusEnum.ENABLED : UserStatusEnum.DISABLED);

        UserContext.setUser(user);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        UserContext.clear();
    }
}
