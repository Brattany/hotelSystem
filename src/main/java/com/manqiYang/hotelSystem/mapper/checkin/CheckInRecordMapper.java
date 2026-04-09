package com.manqiYang.hotelSystem.mapper.checkin;

import com.manqiYang.hotelSystem.entity.checkin.CheckInRecord;
import com.manqiYang.hotelSystem.mapper.base.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

@Mapper
public interface CheckInRecordMapper extends BaseMapper<CheckInRecord, Long> {

    List<CheckInRecord> selectByReservationId(Long reservationId);

    CheckInRecord selectByRoom(String roomNumber);

    List<CheckInRecord> selectByTime(Date checkInTime);
}

