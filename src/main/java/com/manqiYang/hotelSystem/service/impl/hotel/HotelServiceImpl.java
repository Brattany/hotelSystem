package com.manqiYang.hotelSystem.service.impl.hotel;

import com.manqiYang.hotelSystem.entity.hotel.Hotel;
import com.manqiYang.hotelSystem.entity.hotel.HotelTag;
import com.manqiYang.hotelSystem.mapper.hotel.HotelMapper;
import com.manqiYang.hotelSystem.mapper.hotel.HotelTagMapper;
import com.manqiYang.hotelSystem.service.hotel.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

@Service
public class HotelServiceImpl implements HotelService {

    @Autowired
    private HotelMapper hotelMapper;

    @Autowired
    private HotelTagMapper hotelTagMapper;

    @Override
    public Hotel getById(Long hotelId) {
        return hotelMapper.selectById(hotelId);
    }

    @Override
    public List<Hotel> getByName(String hotelName){
        List<Hotel> list = hotelMapper.selectByName(hotelName);
        return list != null ? list : new ArrayList<>();
    }

    @Override
    public List<Hotel> getByCity(String city){
        return hotelMapper.selectByCity(city);
    }

    @Override
    public List<Hotel> getByPriceRange(BigDecimal minPrice, BigDecimal maxPrice){
        return hotelMapper.selectByPriceRange(minPrice, maxPrice);
    }

    @Override
    public List<Hotel> getAll() {
        return hotelMapper.selectAll();
    }

    @Override
    public boolean createHotel(Hotel hotel) {
        hotel.setIsDelete(0);
        return hotelMapper.insert(hotel);
    }

    @Override
    public boolean updateHotel(Hotel hotel) {
        return hotelMapper.updateById(hotel);
    }

    @Override
    public boolean deleteHotel(Long hotelId) {
        return hotelMapper.deleteById(hotelId);
    }

    @Override
    public boolean deleteTag(Long tagId) {
        return hotelTagMapper.deleteById(tagId);
    }

    @Override
    public HotelTag addHotelTag(HotelTag tag) {
        hotelTagMapper.newTag(tag);
        return tag;
    }

    @Override
    public boolean updateHotelTag(HotelTag tag) {
        return hotelTagMapper.updateById(tag);
    }

    @Override
    public List<HotelTag> getTagsByHotelId(Long hotelId) {
        return hotelTagMapper.selectByHotelId(hotelId);
    }
}