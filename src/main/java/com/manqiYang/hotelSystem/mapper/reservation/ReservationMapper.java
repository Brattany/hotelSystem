package com.manqiYang.hotelSystem.mapper.reservation;

import com.manqiYang.hotelSystem.entity.reservation.Reservation;
import com.manqiYang.hotelSystem.entity.room.RoomType;
import com.manqiYang.hotelSystem.enums.reservation.ReservationEnum;
import com.manqiYang.hotelSystem.mapper.base.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation, Long> {

    int updateRoomType(Long reservationId, RoomType type);

    int updateCheckInDate(Long reservationId, Date checkInDate);

    int updateCheckOutDate(Long reservationId, Date checkOutDate);

    int updateStatus(Long reservationId, Integer status);

    int increaseOccupied(Long reservationId);

    int decreaseOccupied(Long reservationId);

    List<Reservation> selectByHotelId(Long hotelId);

    List<Reservation> selectByStatus(Long hotelId, Integer status);

    List<Reservation> selectByDateRange(Long hotelId, Date startDate, Date endDate);

    List<Reservation> selectByGuestId(Long guestId);

    String selectRoomType(Long id);

    List<Reservation> selectAll();

    List<Reservation> selectByGuestPhone(String phone);
}
