package com.manqiYang.hotelSystem.vo;
import lombok.Data;

@Data
public class RoomTypeVO {
    private Long typeId;
    private String typeName;
    private Double price;
    private Integer capacity;
    private String description;
    private String picture;
    private Integer availableCount;
}
