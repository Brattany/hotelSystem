package com.manqiYang.hotelSystem.mapper.room;

import com.manqiYang.hotelSystem.entity.room.RoomType;
import com.manqiYang.hotelSystem.mapper.base.BaseMapper;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RoomTypeMapper extends BaseMapper<RoomType, Long> {

    List<RoomType> selectAvailableRoomTypes(Long hotelId, LocalDate chekInDate, LocalDate checkOutDate);

    List<RoomType> selectByHotelId(Long hotelId);

    List<RoomType> selectByPrice(Integer utterPrice);

    List<RoomType> selectByName(String name);

    void increaseCapacity(Long typeId, Long hotelId);

    void decreaseCapacity(Long typeId, Long hotelId);

    int updateInfo(RoomType roomType);
}

