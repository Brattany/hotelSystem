package com.manqiYang.hotelSystem.entity.hotel;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Hotel {

    private Long hotelId;

    private String hotelName;

    private String province;

    private String city;

    private String district;

    private String address;

    private String phone;

    private BigDecimal priceMin;

    private BigDecimal priceMax;

    private BigDecimal rating;

    private Integer hasWifi;

    private Integer hasBreakfast;

    private Integer hasParking;

    private String cancelPolicy;

    private Integer freeCancel;

    private String description;

    private String picture;

    /**
     * 状态：1-启用 0-停用
     */
    private Integer status;

    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
