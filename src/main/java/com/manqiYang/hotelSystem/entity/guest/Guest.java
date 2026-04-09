package com.manqiYang.hotelSystem.entity.guest;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Guest {

    private Long guestId;

    private String name;

    private String phone;

    private String idCard;

    private String openId;

    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}