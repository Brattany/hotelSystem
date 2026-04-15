package com.manqiYang.hotelSystem.dto.agent;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AgentHotelSearchRequest {

    private AgentHotelLocationQuery location;

    private AgentHotelFacilityQuery facilities;

    private AgentHotelRoomTypeQuery roomType;

    private String hotelName;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private BigDecimal rating;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    private Integer roomCount;
}
