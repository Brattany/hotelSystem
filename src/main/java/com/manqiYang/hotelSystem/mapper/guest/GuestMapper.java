package com.manqiYang.hotelSystem.mapper.guest;

import com.manqiYang.hotelSystem.entity.guest.Guest;
import com.manqiYang.hotelSystem.mapper.base.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GuestMapper extends BaseMapper<Guest, Long> {

    Guest selectByPhone(String phone);

    Guest selectByOpenId(String openId);
}
