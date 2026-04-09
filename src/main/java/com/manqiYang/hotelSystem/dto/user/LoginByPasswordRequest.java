package com.manqiYang.hotelSystem.dto.user;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class LoginByPasswordRequest {
    private String phone;
    private String password;
}
