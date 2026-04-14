package com.manqiYang.hotelSystem.service.impl.room;

import com.manqiYang.hotelSystem.dto.room.AvailableRoomTypeRequest;
import com.manqiYang.hotelSystem.dto.room.UpdateInfoRequest;
import com.manqiYang.hotelSystem.dto.room.UpdateRoomTypeRequest;
import com.manqiYang.hotelSystem.entity.room.Room;
import com.manqiYang.hotelSystem.entity.room.RoomType;
import com.manqiYang.hotelSystem.mapper.room.RoomTypeMapper;
import com.manqiYang.hotelSystem.service.room.RoomService;
import com.manqiYang.hotelSystem.service.room.RoomTypeService;
import com.manqiYang.hotelSystem.vo.RoomTypeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class RoomTypeServiceImpl implements RoomTypeService {

    @Autowired
    public RoomTypeMapper roomTypeMapper;

    @Override
    public RoomType getById(Long id){
        return roomTypeMapper.selectById(id);
    }

    @Override
    public List<RoomTypeVO> getAvailableRoomTypes(AvailableRoomTypeRequest availableRoomTypeRequest){
        Long hotelId = availableRoomTypeRequest.getHotelId();
        LocalDate checkInDate = availableRoomTypeRequest.getCheckInDate();
        LocalDate checkOutDate = availableRoomTypeRequest.getCheckOutDate();
        return roomTypeMapper.selectAvailableRoomTypes(hotelId,checkInDate,checkOutDate);
    }

    @Override
    public List<RoomType> getByHotelId(Long hotelId){
        return roomTypeMapper.selectByHotelId(hotelId);
    }

    @Override
    public List<RoomType> getByPrice(Integer utterPrice){
        return roomTypeMapper.selectByPrice(utterPrice);
    }

    @Override
    public List<RoomType> getByName(String name){
        return roomTypeMapper.selectByName(name);
    }

    @Override
    public boolean create(RoomType roomType){
        return roomTypeMapper.insert(roomType);
    }

    @Override
    public void increaseCapacity(Long typeId, Long hotelId){
        roomTypeMapper.increaseCapacity(typeId,hotelId);
    }

    @Override
    public void decreaseCapacity(Long typeId, Long hotelId){
        roomTypeMapper.decreaseCapacity(typeId,hotelId);
    }

    @Override
    public int update(UpdateRoomTypeRequest updateRoomTypeRequest){
        RoomType roomType = new RoomType();
        roomType.setTypeId(updateRoomTypeRequest.getTypeId());
        roomType.setHotelId(updateRoomTypeRequest.getHotelId());
        roomType.setTypeName(updateRoomTypeRequest.getTypeName());
        roomType.setPrice(updateRoomTypeRequest.getPrice());
        roomType.setCapacity(updateRoomTypeRequest.getCapacity());
        roomType.setDescription(updateRoomTypeRequest.getDescription());
        return roomTypeMapper.updateInfo(roomType);
    }

    @Override
    public boolean delete(Long id){
        return roomTypeMapper.deleteById(id);
    }

}
