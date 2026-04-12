package com.manqiYang.hotelSystem.dto.guest;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class LoginRequest {
    String phone;
    String code;
    String openId;
}
