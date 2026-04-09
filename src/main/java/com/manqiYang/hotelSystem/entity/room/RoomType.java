package com.manqiYang.hotelSystem.entity.room;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RoomType {

    private Long typeId;

    private Long hotelId;

    private String typeName;

    private BigDecimal price;

    private Integer capacity;

    private String description;

    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
