package com.manqiYang.hotelSystem.dto.room;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetRoomsRequest {
    private Long hotelId;
    private Integer status;
}
