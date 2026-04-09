package com.manqiYang.hotelSystem.vo;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
class RoomTypeVO {
    Long typeId;
    String typeName;
    Double price;
    Integer capacity;
    Integer availableCount; // ✔ 正确归属
}
