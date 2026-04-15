package com.manqiYang.hotelSystem.vo.agent;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AgentOrderSummaryVO {

    private Long reservationId;

    private String orderNo;

    private Long guestId;

    private Long hotelId;

    private String hotelName;

    private String hotelPhone;

    private String province;

    private String city;

    private String district;

    private String address;

    private String hotelFullAddress;

    private Long roomTypeId;

    private String roomTypeName;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    private Integer roomCount;

    private BigDecimal totalPrice;

    private Integer status;

    private String statusDescription;

    private LocalDateTime createdAt;
}
