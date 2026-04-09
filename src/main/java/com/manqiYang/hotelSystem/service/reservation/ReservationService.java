package com.manqiYang.hotelSystem.service.reservation;

import com.manqiYang.hotelSystem.entity.reservation.Reservation;
import com.manqiYang.hotelSystem.entity.room.RoomType;
import com.manqiYang.hotelSystem.enums.reservation.ReservationEnum;

import java.util.Date;
import java.util.List;

public interface ReservationService {

    boolean create(Reservation reservation);

    int updateRoomType(Long id, RoomType type);

    int updateCheckInDate(Long id, Date checkInDate);

    int updateCheckOutDate(Long id, Date checkOutDate);

    int updateStatus(Long id, Integer status);

    int increaseOccupied(Long reservationId);

    int decreaseOccupied(Long reservationId);

    Reservation getById(Long id);

    List<Reservation> getByHotelId(Long hotelId);

    List<Reservation> getByStatus(Long hotelId, Integer status);

    List<Reservation> getByDateRange(Long hotelId, Date startDate, Date endDate);

    String getRoomType(Long id);

    boolean delete(Long id);

    List<Reservation> getByGuestPhone(String phone);
}
