package com.manqiYang.hotelSystem.enums.agent;

import com.manqiYang.hotelSystem.enums.base.BaseEnum;

public enum KnowledgeTypeEnum implements BaseEnum<String> {
    POLICY("POLICY", "政策规则"),
    ACTION("ACTION", "动作定义"),
    TEMPLATE("TEMPLATE", "回复模板"),
    FAQ("FAQ", "常见问题");

    private final String code;
    private final String desc;

    KnowledgeTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
