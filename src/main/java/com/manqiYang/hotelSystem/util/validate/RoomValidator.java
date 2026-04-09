package com.manqiYang.hotelSystem.util.validate;

import com.manqiYang.hotelSystem.entity.room.Room;

public class RoomValidator {
    public static void validateRoom(Room room){
        if(room == null){
            throw new IllegalArgumentException("房间不能为空");
        }

        if(room.getRoomNumber() == null){
            throw new IllegalArgumentException("房间号不能为空");
        }

        if(room.getTypeId() == null){
            throw new IllegalArgumentException("房型不饿能为空");
        }
    }
}
