package com.manqiYang.hotelSystem.enums.reservation;

import com.manqiYang.hotelSystem.enums.base.BaseEnum;

public enum ReservationEnum implements BaseEnum<Integer>{
    CREATED(1,"已创建"),
    CONFIRMED(2,"已确认"),
    CANCELED(3,"已取消"),
    CHECKEDIN(4,"已入住"),
    COMPLETED(5,"已完成");

    private Integer code;
    private String desc;

    ReservationEnum(Integer code, String desc){
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {return code; }
    public String getDesc() {return desc; }
}