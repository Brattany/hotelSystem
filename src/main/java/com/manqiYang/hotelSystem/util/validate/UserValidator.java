package com.manqiYang.hotelSystem.util.validate;

public class UserValidator {
    public static void validateUsername(String username) {
        if (!RegexUtil.isUsername(username)) {
            throw new IllegalArgumentException("用户名需3-16位字母/数字/下划线");
        }
    }

    public static void validatePhone(String phone) {
        if (!RegexUtil.isPhone(phone)) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
    }

    public static void validatePassword(String password) {
        if (!RegexUtil.isPassword(password)) {
            throw new IllegalArgumentException("密码需6-20位且包含字母+数字");
        }
    }

    public static void validateNotSamePassword(String oldPwd, String newPwd) {
        if (oldPwd.equals(newPwd)) {
            throw new IllegalArgumentException("新密码不能与旧密码相同");
        }
    }
}
