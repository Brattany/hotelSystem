package com.manqiYang.hotelSystem.dto.user;

import com.manqiYang.hotelSystem.entity.user.SysUser;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class RegisterRequest {
    Long hotelId;
    String userName;
    String password;
    String role;
    String phone;
    String code;
}
