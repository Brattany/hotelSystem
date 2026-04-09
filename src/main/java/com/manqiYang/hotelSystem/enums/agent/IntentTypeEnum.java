package com.manqiYang.hotelSystem.enums.agent;

import com.manqiYang.hotelSystem.enums.base.BaseEnum;

public enum IntentTypeEnum implements BaseEnum<String> {
    RECOMMEND("RECOMMEND", "推荐"),
    POLICY("POLICY", "政策"),
    ORDER("ORDER", "订单");

    private final String code;
    private final String desc;

    IntentTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
