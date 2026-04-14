package com.manqiYang.hotelSystem.dto.guest;

import lombok.Data;

@Data
public class GuestProfileResponse {

    private Long guestId;

    private String name;

    private String phone;

    private String idCard;

    private String token;
}
