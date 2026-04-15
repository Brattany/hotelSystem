package com.manqiYang.hotelSystem.dto.agent;

import lombok.Data;

@Data
public class AgentHotelLocationQuery {

    private String province;

    private String city;

    private String district;

    private String districtKeyword;

    private String street;

    private String addressKeyword;
}
