package com.manqiYang.hotelSystem.enums.agent;

import com.manqiYang.hotelSystem.enums.base.BaseEnum;

public enum ActionStatusEnum implements BaseEnum<Integer> {
    SUCCESS(1, "成功"),
    FAILURE(0, "失败");

    private final Integer code;
    private final String desc;

    ActionStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }
    public String getDesc() { return desc; }
}
