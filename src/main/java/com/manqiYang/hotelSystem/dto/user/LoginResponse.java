package com.manqiYang.hotelSystem.dto.user;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class LoginResponse {
    private Long userId;
    private String username;
    private String token;
}
