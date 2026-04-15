package com.manqiYang.hotelSystem.dto.agent;

import lombok.Data;

@Data
public class AgentOrderSearchRequest {

    private Long guestId;

    private Long reservationId;

    private Integer recentDays;

    private String province;

    private String city;

    private String district;

    private String hotelName;

    private Long roomTypeId;

    private String roomTypeKeyword;

    private String status;

    private Integer limit;

    private String sort;
}
