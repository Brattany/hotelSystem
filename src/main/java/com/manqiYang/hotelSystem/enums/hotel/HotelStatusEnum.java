package com.manqiYang.hotelSystem.enums.hotel;

import com.manqiYang.hotelSystem.enums.base.BaseEnum;

public enum HotelStatusEnum implements BaseEnum<Integer> {

    DISABLED(0, "停用"),
    ENABLED(1, "启用");

    private final Integer code;
    private final String desc;

    HotelStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }
    public String getDesc() { return desc; }
}
