package com.manqiYang.hotelSystem.model;

import com.manqiYang.hotelSystem.enums.user.UserStatusEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserInfo {
    // getter/setter
    private Long userId;
    private Long hotelId;
    private String username;
    private UserStatusEnum status;
}