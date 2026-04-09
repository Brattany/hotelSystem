package com.manqiYang.hotelSystem.entity.agent;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentSession {

    private String sessionId;

    private Long guestId;

    private String currentCity;

    private Long currentHotelId;

    private Long currentReservationId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
