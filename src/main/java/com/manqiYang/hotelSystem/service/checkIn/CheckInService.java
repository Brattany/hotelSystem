package com.manqiYang.hotelSystem.service.checkIn;

import com.manqiYang.hotelSystem.dto.order.CheckInRequest;
import com.manqiYang.hotelSystem.entity.checkin.CheckInRecord;

import java.util.Date;
import java.util.List;

public interface CheckInService {
    CheckInRecord getById(Long id);

    List<CheckInRecord> getByReservationId(Long id);

    CheckInRecord getByRoom(String roomNumber);

    List<CheckInRecord> getByTime(Date checkInTime);

    boolean create(CheckInRequest checkInRequest);

    boolean delete(Long id);
}
