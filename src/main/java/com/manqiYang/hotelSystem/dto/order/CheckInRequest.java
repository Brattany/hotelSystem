package com.manqiYang.hotelSystem.dto.order;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CheckInRequest {
    private Long hotelId;
    private Long reservationId;
    private Integer realCount;
    private String realInfo;
    private List<String> roomNumbers;
}
