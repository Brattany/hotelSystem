package com.manqiYang.hotelSystem.enums.agent;

import com.manqiYang.hotelSystem.enums.base.BaseEnum;

public enum ApplicableScopeEnum implements BaseEnum<String> {
    GLOBAL("GLOBAL","全局适用"),
    HOTEL("HOTEL","酒店级"),
    ROOM_TYPE("ROOM_TYPE","房型级");

    private String code;
    private String desc;

    ApplicableScopeEnum(String code, String desc){
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {return code; }
    public String getDesc() {return desc; }
}
