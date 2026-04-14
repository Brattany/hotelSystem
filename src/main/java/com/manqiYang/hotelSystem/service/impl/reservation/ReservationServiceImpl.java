package com.manqiYang.hotelSystem.service.impl.reservation;

import com.manqiYang.hotelSystem.dto.order.CreateReservationResponse;
import com.manqiYang.hotelSystem.dto.order.GetReservationByPhoneResponse;
import com.manqiYang.hotelSystem.entity.reservation.Reservation;
import com.manqiYang.hotelSystem.entity.room.RoomType;
import com.manqiYang.hotelSystem.enums.reservation.ReservationEnum;
import com.manqiYang.hotelSystem.mapper.reservation.ReservationMapper;
import com.manqiYang.hotelSystem.service.reservation.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ReservationServiceImpl implements ReservationService {

    @Autowired
    public ReservationMapper reservationMapper;

    @Override
    public CreateReservationResponse create(Reservation reservation){
        int rows = reservationMapper.insert(reservation);

        if (rows > 0) {
            Long reservationId = reservation.getReservationId();
            if (reservationId == null) {
                return null;
            }

            CreateReservationResponse response = new CreateReservationResponse();
            response.setReservationId(reservationId);
            response.setOrderNo(String.valueOf(reservationId));
            return response;
        }

        return null;
    }

    @Override
    public int updateRoomType(Long id, RoomType type){
        return reservationMapper.updateRoomType(id,type);
    }

    @Override
    public int updateCheckInDate(Long id, Date checkInDate){
        return reservationMapper.updateCheckInDate(id,checkInDate);
    }

    @Override
    public int updateCheckOutDate(Long id, Date checkOutDate){
        return reservationMapper.updateCheckOutDate(id,checkOutDate);
    }

    @Override
    public int updateStatus(Long id, Integer status){
        return reservationMapper.updateStatus(id,status);
    }

    @Override
    public int increaseOccupied(Long reservationId){
        return reservationMapper.increaseOccupied(reservationId);
    }

    @Override
    public int decreaseOccupied(Long reservationId){
        return reservationMapper.decreaseOccupied(reservationId);
    }

    @Override
    public Reservation getById(Long id){
        return reservationMapper.selectById(id);
    }

    @Override
    public List<Reservation> getByHotelId(Long hoteId){
        return reservationMapper.selectByHotelId(hoteId);
    }

    @Override
    public List<Reservation> getByStatus(Long hotelId, Integer status){
        return reservationMapper.selectByStatus(hotelId,status);
    }

    @Override
    public List<Reservation> getByDateRange(Long hotelId, Date startDate, Date endDate){
        return reservationMapper.selectByDateRange(hotelId,startDate,endDate);
    }

    @Override
    public String getRoomType(Long id){
        return reservationMapper.selectRoomType(id);
    }

    @Override
    public boolean delete(Long id){
        return reservationMapper.deleteById(id);
    }

    @Override
    public List<GetReservationByPhoneResponse> getByGuestPhone(Long hotelId, String phone) {
        return reservationMapper.selectByGuestPhone(hotelId, phone);
    }

    @Override
    public List<GetReservationByPhoneResponse> getAllByPhone(String phone){
        return reservationMapper.selectAllByPhone(phone);
    }
}
