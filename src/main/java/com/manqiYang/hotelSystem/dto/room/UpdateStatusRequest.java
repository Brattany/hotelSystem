package com.manqiYang.hotelSystem.dto.room;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatusRequest {
    Long roomId;
    Integer status;
}
