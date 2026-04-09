package com.manqiYang.hotelSystem.entity.hotel;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HotelTag {

    private Long id;

    private Long hotelId;

    private String tag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
