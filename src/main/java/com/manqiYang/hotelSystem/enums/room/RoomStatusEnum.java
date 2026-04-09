package com.manqiYang.hotelSystem.enums.room;

import com.manqiYang.hotelSystem.enums.base.BaseEnum;

public enum RoomStatusEnum implements BaseEnum<Integer>{
    AVAILABLE(1,"可住"),
    OCCUPIED(2,"已入住"),
    MAINTENANCE(3,"维修中");

    private Integer code;
    private String desc;

    RoomStatusEnum(Integer code, String desc){
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {return code; }
    public String getDesc() {return desc; }
}
