package com.manqiYang.hotelSystem.service.guest;

import com.manqiYang.hotelSystem.dto.guest.GuestProfileResponse;
import com.manqiYang.hotelSystem.dto.guest.RegisterRequest;
import com.manqiYang.hotelSystem.entity.guest.Guest;
import com.manqiYang.hotelSystem.dto.guest.LoginRequest;

import java.util.List;

public interface GuestService {

    boolean register(RegisterRequest registerRequest);

    String login(LoginRequest loginRequest);

    String wxLogin(String wxCode);//微信静默登录

    String sendCode(String phone);

    boolean create(Guest guest);

    Guest getById(Long guestId);

    Guest getByPhone(String phone);

    Guest getByOpenId(String openId);

    GuestProfileResponse update(Guest guest);

    boolean delete(Long guestId);
}
