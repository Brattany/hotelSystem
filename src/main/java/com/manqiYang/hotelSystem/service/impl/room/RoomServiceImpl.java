package com.manqiYang.hotelSystem.service.impl.room;

import com.manqiYang.hotelSystem.context.UserContext;
import com.manqiYang.hotelSystem.dto.room.GetByTSRequest;
import com.manqiYang.hotelSystem.dto.room.GetRoomsRequest;
import com.manqiYang.hotelSystem.dto.room.UpdateInfoRequest;
import com.manqiYang.hotelSystem.entity.room.Room;
import com.manqiYang.hotelSystem.mapper.room.RoomMapper;
import com.manqiYang.hotelSystem.mapper.room.RoomTypeMapper;
import com.manqiYang.hotelSystem.service.room.RoomService;
import com.manqiYang.hotelSystem.util.validate.RoomValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomServiceImpl implements RoomService {

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private RoomTypeMapper roomTypeMapper;

    @Override
    public Room getById(Long roomId) {
        return roomMapper.selectById(roomId);
    }


    /*****************************/
    /****获取当前酒店某一房间的信息****/
    /*****************************/
    @Override
    public Room getByRoomNumber(String roomNumber, Long hotelId){
        if(roomNumber == null){
            throw new IllegalArgumentException("房间号不能为空");
        }

        return roomMapper.selectByRoomNumber(roomNumber, hotelId);
    }

    /**************************/
    /******获取当前酒店的房间******/
    /**************************/
    @Override
    public List<Room> getByHotel(Long hotelId){
        return roomMapper.selectByHotelId(hotelId);
    }

    /*************************************/
    /****获取当前酒店某一类型的所有房间的信息****/
    /*************************************/
    @Override
    public List<Room> getByType(Long typeId){
        Long hotelId=  UserContext.getHotelId();

        if(typeId == null){
            throw new IllegalArgumentException("类型不能为空");
        }

        return roomMapper.selectByTypeId(typeId,hotelId);
    }

    /*************************************/
    /****获取当前酒店某一状态的所有房间的信息****/
    /*************************************/
    @Override
    public List<Room> getByStatus(GetRoomsRequest getRoomsRequest){
        Long hotelId = getRoomsRequest.getHotelId();
        Integer status = getRoomsRequest.getStatus();

        if(status == null){
            throw new IllegalArgumentException("状态非法");
        }

        return roomMapper.selectByStatus(status,hotelId);
    }

    /*************************************/
    /****获取当前酒店某一状态的某类型房间的信息****/
    /*************************************/
    @Override
    public List<Room> getByTAndS(GetByTSRequest getByTSRequest){
        Long typeId = getByTSRequest.getTypeId();
        Long hotelId = getByTSRequest.getHotelId();
        Integer status = getByTSRequest.getStatus();

        if(status == null){
            throw new IllegalArgumentException("状态非法");
        }

        return roomMapper.selectByTAndS(typeId,status,hotelId);
    }

    /***********************/
    /****在当前酒店新增房间****/
    /**********************/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean create(Room room) {
        if(room.getStatus() == null){
            room.setStatus(1); // AVAILABLE
        }

        boolean result=roomMapper.insert(room);
        if(!result){
            throw new RuntimeException("新增失败");
        }
        else{
            //更新房型总量
            roomTypeMapper.increaseCapacity(room.getTypeId(), room.getHotelId());
        }
        return result;
    }

    /******************/
    /****更新房间状态****/
    /******************/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long roomId, Integer status){

        Long hotelId = UserContext.getHotelId();

        Room room = roomMapper.selectById(roomId);
        if(room == null || !room.getHotelId().equals(hotelId)){
            throw new RuntimeException("无权限操作该房间");
        }

        return roomMapper.updateStatus(roomId, status);
    }

    /******************/
    /****更新房间信息****/
    /******************/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateInfo(UpdateInfoRequest updateInfoRequest) {
        Long hotelId = updateInfoRequest.getHotelId();

        Room existingRoom = roomMapper.selectById(updateInfoRequest.getRoomId());
        if(existingRoom == null || !existingRoom.getHotelId().equals(hotelId)){
            throw new RuntimeException("无权限操作该房间");
        }

        // 仅允许更新房间号和类型
        existingRoom.setRoomNumber(updateInfoRequest.getRoomNumber());
        existingRoom.setTypeId(updateInfoRequest.getTypeId());
        existingRoom.setStatus(updateInfoRequest.getStatus());

        return roomMapper.updateById(existingRoom);
    }

    /*************************************/
    /*********删除房间（校验归属）************/
    /*************************************/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long roomId) {

        Room room = roomMapper.selectById(roomId);
        Long hotelId = room.getHotelId();
        Long typeId = room.getTypeId();

        if(room == null || !room.getHotelId().equals(hotelId)){
            throw new RuntimeException("无权限删除该房间");
        }
        else if(room.getStatus() == 2){ // OCCUPIED
            throw new RuntimeException("入住中，不可删除");
        }
        else{
            //更新房型总量
            roomTypeMapper.decreaseCapacity(typeId, hotelId);
        }
        return roomMapper.deleteById(roomId);
    }
}