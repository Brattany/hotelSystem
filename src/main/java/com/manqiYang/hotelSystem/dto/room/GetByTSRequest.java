package com.manqiYang.hotelSystem.dto.room;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetByTSRequest {
    private Long hotelId;
    private Long typeId;
    private Integer status;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
}
