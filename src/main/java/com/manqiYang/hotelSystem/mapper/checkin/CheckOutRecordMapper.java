package com.manqiYang.hotelSystem.mapper.checkin;

import com.manqiYang.hotelSystem.entity.checkin.CheckOutRecord;
import com.manqiYang.hotelSystem.mapper.base.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CheckOutRecordMapper extends BaseMapper<CheckOutRecord, Long> {
    List<CheckOutRecord> selectAll();

    CheckOutRecord selectByCheckInId(Long checkInId);
}

