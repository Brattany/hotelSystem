package com.manqiYang.hotelSystem.service.impl.checkIn;

import com.manqiYang.hotelSystem.entity.checkin.CheckOutRecord;
import com.manqiYang.hotelSystem.entity.checkin.CheckInRecord;
import com.manqiYang.hotelSystem.entity.reservation.Reservation;
import com.manqiYang.hotelSystem.entity.room.Room;
import com.manqiYang.hotelSystem.mapper.checkin.CheckInRecordMapper;
import com.manqiYang.hotelSystem.mapper.checkin.CheckOutRecordMapper;
import com.manqiYang.hotelSystem.mapper.reservation.ReservationMapper;
import com.manqiYang.hotelSystem.mapper.room.RoomMapper;
import com.manqiYang.hotelSystem.service.checkIn.CheckOutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CheckOutServiceImpl implements CheckOutService {

    @Autowired
    public CheckOutRecordMapper outMapper;

    @Autowired
    public CheckInRecordMapper inMapper;

    @Autowired
    public ReservationMapper reservationMapper;

    @Autowired
    public RoomMapper roomMapper;

    @Override
    public boolean create(String roomNumber){

        CheckInRecord inRecord = inMapper.selectByRoom(roomNumber);
        boolean success = true;
        if(inRecord == null){
            return false;
        }

        Long checkInId = inRecord.getCheckInId();
        Long hotelId = inRecord.getHotelId();
        Long reservationId = inRecord.getReservationId();

        CheckOutRecord outRecord = new CheckOutRecord();
        outRecord.setCheckInId(checkInId);
        outRecord.setHotelId(hotelId);

        if(outMapper.insert(outRecord)){
            //修改房间状态为可用
            Room room = roomMapper.selectByRoomNumber(roomNumber,hotelId);
            roomMapper.updateStatus(room.getRoomId(),1);
            //修改订单占用房间数量
            Reservation reservation = reservationMapper.selectById(inRecord.getReservationId());
            reservationMapper.decreaseOccupied(reservationId);
            if(reservation.getOccupiedRooms() == 0){
                //修改订单状态为已完成
                reservationMapper.updateStatus(reservationId,5);
            }
        }
        return success;
    }

    @Override
    public List<CheckOutRecord> getAll(){
        return outMapper.selectAll();
    }

    @Override
    public CheckOutRecord getByCheckInId(Long checkInId){
        return outMapper.selectByCheckInId(checkInId);
    }

    @Override
    public CheckOutRecord getById(Long id){
        return outMapper.selectById(id);
    }

    @Override
    public boolean delete(Long id){
        return outMapper.deleteById(id);
    }
}
