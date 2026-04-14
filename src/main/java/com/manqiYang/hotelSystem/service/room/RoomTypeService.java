package com.manqiYang.hotelSystem.service.room;

import com.manqiYang.hotelSystem.dto.room.AvailableRoomTypeRequest;
import com.manqiYang.hotelSystem.dto.room.UpdateRoomTypeRequest;
import com.manqiYang.hotelSystem.entity.room.Room;
import com.manqiYang.hotelSystem.entity.room.RoomType;
import com.manqiYang.hotelSystem.vo.RoomTypeVO;

import java.util.List;

public interface RoomTypeService {

    RoomType getById(Long id);

    List<RoomTypeVO> getAvailableRoomTypes(AvailableRoomTypeRequest availableRoomTypeRequest);

    List<RoomType> getByHotelId(Long hotelId);

    List<RoomType> getByPrice(Integer utterPrice);

    List<RoomType> getByName(String name);

    boolean create(RoomType roomType);

    void increaseCapacity(Long typeId, Long hotelId);

    void decreaseCapacity(Long typeId, Long hotelId);

    int update(UpdateRoomTypeRequest updateRoomTypeRequest);

    boolean delete(Long id);
}
