package com.manqiYang.hotelSystem.service.checkIn;

import com.manqiYang.hotelSystem.entity.checkin.CheckInRecord;
import com.manqiYang.hotelSystem.entity.checkin.CheckOutRecord;

import java.util.List;

public interface CheckOutService {

    boolean create(String roomNumber);

    List<CheckOutRecord> getAll();

    CheckOutRecord getByCheckInId(Long checkInId);

    CheckOutRecord getById(Long id);

    boolean delete(Long id);
}
