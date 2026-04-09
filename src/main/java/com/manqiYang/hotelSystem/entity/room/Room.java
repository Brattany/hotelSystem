package com.manqiYang.hotelSystem.entity.room;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Room {

    private Long roomId;

    private Long hotelId;

    private Long typeId;

    private String roomNumber;

    /**
     * 状态：1-available/2-occupied/3-maintenance
     */
    private Integer status;

    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
