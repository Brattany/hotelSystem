package com.manqiYang.hotelSystem.mapper.room;

import com.manqiYang.hotelSystem.entity.room.RoomType;
import com.manqiYang.hotelSystem.mapper.base.BaseMapper;
import com.manqiYang.hotelSystem.vo.RoomTypeVO;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RoomTypeMapper extends BaseMapper<RoomType, Long> {

    List<RoomTypeVO> selectAvailableRoomTypes(@Param("hotelId") Long hotelId,
                                              @Param("checkInDate") LocalDate checkInDate,
                                              @Param("checkOutDate") LocalDate checkOutDate);

    List<RoomType> selectByHotelId(Long hotelId);

    List<RoomType> selectByPrice(Integer utterPrice);

    List<RoomType> selectByName(String name);

    void increaseCapacity(Long typeId, Long hotelId);

    void decreaseCapacity(Long typeId, Long hotelId);

    int updateInfo(RoomType roomType);
}

