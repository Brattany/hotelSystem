package com.manqiYang.hotelSystem.enums.base;

public enum OperatorTypeEnum implements BaseEnum<Integer> {
    STAFF(1, "员工"),
    GUEST(2, "顾客"),
    AGENT(3, "智能体");

    private Integer code;
    private String desc;

    OperatorTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() { return code; }
    public String getDesc() { return desc; }
}
