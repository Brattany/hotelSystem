package com.manqiYang.hotelSystem.mapper.room;

import com.manqiYang.hotelSystem.entity.room.Room;
import com.manqiYang.hotelSystem.mapper.base.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RoomMapper extends BaseMapper<Room, Long> {

    Room selectByRoomNumber(@Param("roomNumber") String roomNumber,
                            @Param("hotelId") Long hotelId);

    List<Room> selectByHotelId(Long hotelId);

    List<Room> selectByTypeId(@Param("typeId") Long typeId,
                              @Param("hotelId") Long hotelId);

    List<Room> selectByStatus(@Param("status") Integer status,
                              @Param("hotelId") Long hotelId);

    List<Room> selectByTAndS(@Param("hotelId") Long hotelId,
                             @Param("typeId") Long typeId,
                             @Param("status") Integer status,
                             @Param("checkInDate") LocalDate checkInDate,
                             @Param("checkOutDate") LocalDate checkOutDate);

    boolean updateStatus(@Param("roomId") Long roomId,
                         @Param("status") Integer status);

}
