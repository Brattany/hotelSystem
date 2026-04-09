package com.manqiYang.hotelSystem.service.impl.guest;

import com.manqiYang.hotelSystem.entity.guest.Guest;
import com.manqiYang.hotelSystem.mapper.guest.GuestMapper;
import com.manqiYang.hotelSystem.service.guest.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GuestServiceImpl implements GuestService {

    @Autowired
    public GuestMapper guestMapper;

    @Override
    public boolean register(Guest guest) {
        guest.setIsDelete(0);
        return guestMapper.insert(guest);
    }

    @Override
    public Guest login(String openId) {
        return guestMapper.selectByOpenId(openId);
    }

    @Override
    public boolean create(Guest guest){
        guest.setIsDelete(0);
        return guestMapper.insert(guest);
    }

    @Override
    public Guest getById(Long guestId){
        return guestMapper.selectById(guestId);
    }

    @Override
    public Guest getByPhone(String phone){
        return guestMapper.selectByPhone(phone);
    }

    @Override
    public Guest getByOpenId(String openId){
        return guestMapper.selectByOpenId(openId);
    }

    @Override
    public boolean update(Guest guest){
        return guestMapper.updateById(guest);
    }

    @Override
    public boolean delete(Long guestId){
        return guestMapper.deleteById(guestId);
    }
}
