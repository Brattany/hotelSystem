package com.manqiYang.hotelSystem.service.room;

import com.manqiYang.hotelSystem.dto.room.GetByTSRequest;
import com.manqiYang.hotelSystem.dto.room.GetRoomsRequest;
import com.manqiYang.hotelSystem.dto.room.UpdateInfoRequest;
import com.manqiYang.hotelSystem.entity.room.Room;

import java.util.List;

public interface RoomService {

    Room getById(Long roomId);

    Room getByRoomNumber(String roomNumber,Long hotelId);

    List<Room> getByHotel(Long hotelId);

    List<Room> getByType(Long typeId);

    List<Room> getByStatus(GetRoomsRequest getRoomsRequest);

    List<Room> getByTAndS(GetByTSRequest getByTSRequest);

    boolean create(Room room);

    boolean updateStatus(Long roomId, Integer status);

    boolean updateInfo(UpdateInfoRequest updateInfoRequest);

    boolean delete(Long id);
}