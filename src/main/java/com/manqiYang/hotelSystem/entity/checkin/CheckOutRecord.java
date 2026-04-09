package com.manqiYang.hotelSystem.entity.checkin;

import lombok.Data;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class CheckOutRecord {

    private Long checkOutId;

    private Long hotelId;

    private Long checkInId;

    private LocalDateTime checkOutTime;

    private LocalDateTime createTime;
}
