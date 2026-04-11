package com.manqiYang.hotelSystem.dto.user;

import com.manqiYang.hotelSystem.entity.user.SysUser;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeRequest {
    Long userId;
    String oldPass;
    String newPass;
}
