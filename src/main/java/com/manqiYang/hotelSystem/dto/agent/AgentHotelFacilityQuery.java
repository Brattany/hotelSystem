package com.manqiYang.hotelSystem.dto.agent;

import lombok.Data;

import java.util.List;

@Data
public class AgentHotelFacilityQuery {

    private List<String> required;

    private String matchMode;

    private Integer minMatchCount;
}
