package com.manqiYang.hotelSystem.mapper.hotel;

import com.manqiYang.hotelSystem.entity.hotel.Hotel;
import com.manqiYang.hotelSystem.mapper.base.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface HotelMapper extends BaseMapper<Hotel, Long> {

    List<Hotel> selectAll();

    List<Hotel> selectByName(String hotelName);

    List<Hotel> selectByCity(String city);

    List<Hotel> selectByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);
}
