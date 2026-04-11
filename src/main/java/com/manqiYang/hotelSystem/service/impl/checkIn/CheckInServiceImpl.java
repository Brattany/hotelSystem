package com.manqiYang.hotelSystem.service.impl.checkIn;

import com.manqiYang.hotelSystem.dto.order.CheckInRequest;
import com.manqiYang.hotelSystem.entity.checkin.CheckInRecord;
import com.manqiYang.hotelSystem.entity.room.Room;
import com.manqiYang.hotelSystem.mapper.checkin.CheckInRecordMapper;
import com.manqiYang.hotelSystem.mapper.reservation.ReservationMapper;
import com.manqiYang.hotelSystem.mapper.room.RoomMapper;
import com.manqiYang.hotelSystem.service.checkIn.CheckInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class CheckInServiceImpl implements CheckInService {
    @Autowired
    public CheckInRecordMapper inMapper;

    @Autowired
    public RoomMapper roomMapper;

    @Autowired
    public ReservationMapper reservationMapper;

    @Override
    public CheckInRecord getById(Long id){
        return inMapper.selectById(id);
    }

    @Override
    public List<CheckInRecord> getByReservationId(Long id){
        return inMapper.selectByReservationId(id);
    }

    @Override
    public CheckInRecord getByRoom(String roomNumber){
        return inMapper.selectByRoom(roomNumber);
    }

    @Override
    public List<CheckInRecord> getByTime(Date checkInTime){
        return inMapper.selectByTime(checkInTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean create(CheckInRequest checkInRequest){
        Long hotelId = checkInRequest.getHotelId();
        Long reservationId = checkInRequest.getReservationId();
        Integer realCount = checkInRequest.getRealCount();
        String realInfo = checkInRequest.getRealInfo();
        List<String> roomNumbers = checkInRequest.getRoomNumbers();
        boolean success = true;

        //修改订单状态为已入住
        reservationMapper.updateStatus(reservationId, 4);

        for(String roomNumber : roomNumbers){
            CheckInRecord record = new CheckInRecord();
            record.setHotelId(hotelId);
            record.setReservationId(reservationId);
            record.setRealCount(realCount);
            record.setRealInfo(realInfo);
            record.setRoomNumber(roomNumber);
            
            if(!inMapper.insert(record)){
                success = false;
            }
            else{
                //修改房间状态为已入住
                Room room = roomMapper.selectByRoomNumber(roomNumber, hotelId);
                roomMapper.updateStatus(room.getRoomId(), 2);
                //修改订单占用房间数量
                reservationMapper.increaseOccupied(reservationId);
            }
        }
        return success;
    }

    @Override
    public boolean delete(Long id){
        return inMapper.deleteById(id);
    }
}
