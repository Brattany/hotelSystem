package com.manqiYang.hotelSystem.dto.order;

import lombok.Data;

@Data
public class CreateReservationResponse {

    private Long reservationId;

    /**
     * 当前系统尚未落库独立 orderNo，先兼容返回可展示的订单号。
     */
    private String orderNo;
}
