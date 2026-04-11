package com.manqiYang.hotelSystem.mapper.hotel;

import com.manqiYang.hotelSystem.entity.hotel.HotelTag;
import com.manqiYang.hotelSystem.mapper.base.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HotelTagMapper extends BaseMapper<HotelTag, Long> {

    List<HotelTag> selectByHotelId(Long hotelId);

    List<HotelTag> selectByTag(String tag);

    int newTag(HotelTag tag);
}
