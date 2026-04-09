package com.manqiYang.hotelSystem.dto.room;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AvailableRoomTypeRequest {
    Long hotelId;
    LocalDate checkInDate;
    LocalDate checkOutDate;
}
