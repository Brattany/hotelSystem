package com.manqiYang.hotelSystem.util.validate;

public class RegexUtil {
    // 支持中文、字母、数字、下划线，3-16位
    private static final String USERNAME_REGEX = "^[\\u4e00-\\u9fa5a-zA-Z0-9_]{3,16}$";
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    private static final String PASSWORD_REGEX =
            "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@#$%^&+=!]{6,20}$";

    public static boolean isUsername(String username) {
        return username != null && username.matches(USERNAME_REGEX);
    }

    public static boolean isPhone(String phone) {
        return phone != null && phone.matches(PHONE_REGEX);
    }

    public static boolean isPassword(String password) {
        return password != null && password.matches(PASSWORD_REGEX);
    }
}
