package com.manqiYang.hotelSystem.entity.checkin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckInRecord {

    private Long checkInId;

    private Long hotelId;

    private Long reservationId;

    /**
     * 实际入住人数
     */
    private Integer realCount;

    /**
     * 实际入住人信息
     */
    private String realInfo;

    private String roomNumber;

    private LocalDateTime checkInTime;


    private LocalDateTime createTime;
}