package com.manqiYang.hotelSystem.dto.agent;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AgentOrderUpdateRequest {

    private Long guestId;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    private Long roomTypeId;

    private String roomTypeKeyword;
}
