package com.manqiYang.hotelSystem.entity.log;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderChangeLog {

    private Long changeId;

    private Long reservationId;

    private Long hotelId;

    private String changeType;

    private String beforeValue;

    private String afterValue;

    /**
     * 操作类型：1-员工 2-顾客 3-智能体
     */
    private Integer operatorType;

    private Long operatorId;

    private LocalDateTime changeTime;
}
