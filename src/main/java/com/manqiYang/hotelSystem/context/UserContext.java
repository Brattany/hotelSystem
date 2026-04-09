package com.manqiYang.hotelSystem.context;

import com.manqiYang.hotelSystem.model.UserInfo;

public class UserContext {

    private static final ThreadLocal<UserInfo> USER_HOLDER = new ThreadLocal<>();

    public static void setUser(UserInfo user) {
        USER_HOLDER.set(user);
    }

    public static UserInfo getUser() {
        UserInfo user = USER_HOLDER.get();
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        return user;
    }

    public static Long getUserId() {
        return getUser().getUserId();
    }

    public static Long getHotelId(){
        return getUser().getHotelId();
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}