package com.manqiYang.hotelSystem.enums.agent;

import com.manqiYang.hotelSystem.enums.base.BaseEnum;

public enum ActionTypeEnum implements BaseEnum<String> {
    CANCEL("CANCEL", "取消"),
    MODIFY("MODIFY", "修改"),
    QUERY("QUERY", "查询");

    private final String code;
    private final String desc;

    ActionTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
