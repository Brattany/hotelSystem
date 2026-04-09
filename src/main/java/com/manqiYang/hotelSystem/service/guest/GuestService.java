package com.manqiYang.hotelSystem.service.guest;

import com.manqiYang.hotelSystem.entity.guest.Guest;

import java.util.List;

public interface GuestService {

    boolean register(Guest guest);

    Guest login(String openId);

    boolean create(Guest guest);

    Guest getById(Long guestId);

    Guest getByPhone(String phone);

    Guest getByOpenId(String openId);

    boolean update(Guest guest);

    boolean delete(Long guestId);
}
