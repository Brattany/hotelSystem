package com.manqiYang.hotelSystem.dto.room;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateRoomTypeRequest {
    private Long typeId;
    private Long hotelId;
    private String typeName;
    private BigDecimal price;
    private Integer capacity;
    private String description;
    private String picture;
}
