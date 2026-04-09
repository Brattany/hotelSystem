package com.manqiYang.hotelSystem.dto.room;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateInfoRequest {
    Long roomId;
    Long hotelId;
    Long typeId;
    String roomNumber;
    Integer status;
}
