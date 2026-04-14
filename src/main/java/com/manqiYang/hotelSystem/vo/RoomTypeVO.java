package com.manqiYang.hotelSystem.vo;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class RoomTypeVO {
    private Long typeId;
    private String typeName;
    private Double price;
    private Integer capacity;
    Integer availableCount; // ✔ 正确归属
}
