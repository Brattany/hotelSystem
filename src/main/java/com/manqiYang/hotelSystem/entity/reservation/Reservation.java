package com.manqiYang.hotelSystem.entity.reservation;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Reservation {

    private Long reservationId;

    private Long hotelId;

    private Long guestId;

    private Long typeId;

    private Integer roomCount;

    private BigDecimal totalPrice;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    private Integer occupiedRooms;

    /**
     * 状态：1-created/2-confirmed/3-canceled/4-checked_in/5-completed
     */
    private Integer status;

    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private LocalDateTime cancelDeadline;
}
