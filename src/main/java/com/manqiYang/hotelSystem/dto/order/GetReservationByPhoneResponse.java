package com.manqiYang.hotelSystem.dto.order;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class GetReservationByPhoneResponse {
    private Long reservationId;
    private String typeId;
    private String guestName;
    private String typeName;
    private Integer roomCount;
    private BigDecimal totalPrice;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer status;
    private Integer occupiedRooms;
}
