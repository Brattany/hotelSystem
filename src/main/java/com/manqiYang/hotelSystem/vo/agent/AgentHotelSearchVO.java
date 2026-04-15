package com.manqiYang.hotelSystem.vo.agent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AgentHotelSearchVO {

    private Long hotelId;

    private String hotelName;

    private String phone;

    private String province;

    private String city;

    private String district;

    private String address;

    private String fullAddress;

    private String description;

    private BigDecimal minPrice;

    private BigDecimal rating;

    private Integer hasWifi;

    private Integer hasBreakfast;

    private Integer hasParking;

    @JsonIgnore
    private String matchedRoomTypesRaw;

    private List<String> matchedRoomTypes;

    private Integer availableRoomCount;

    private String picture;
}
