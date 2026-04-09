package com.manqiYang.hotelSystem.enums.user;

import com.manqiYang.hotelSystem.enums.base.BaseEnum;

public enum UserStatusEnum implements BaseEnum<Integer> {

    DISABLED(0,"禁用"),
    ENABLED(1,"启用");

    private final Integer code;
    private final String desc;

    UserStatusEnum(Integer code, String desc){
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {return code; }
    public String getDesc() {return desc; }
}
