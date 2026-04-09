package com.manqiYang.hotelSystem.service.hotel;

import com.manqiYang.hotelSystem.entity.hotel.Hotel;
import com.manqiYang.hotelSystem.entity.hotel.HotelTag;
import java.math.BigDecimal;
import java.util.List;

public interface HotelService {

    Hotel getById(Long hotelId);

    List<Hotel> getByName(String hotelName);

    List<Hotel> getByCity(String city);

    List<Hotel> getByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);

    List<Hotel> getAll();

    boolean createHotel(Hotel hotel);

    boolean updateHotel(Hotel hotel);

    boolean deleteHotel(Long hotelId);

    // HotelTag operations
    boolean addHotelTag(HotelTag tag);

    boolean updateHotelTag(HotelTag tag);

    List<HotelTag> getTagsByHotelId(Long hotelId);
}