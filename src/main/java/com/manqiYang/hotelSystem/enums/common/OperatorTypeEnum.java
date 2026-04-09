package com.manqiYang.hotelSystem.enums.common;

import com.manqiYang.hotelSystem.enums.base.BaseEnum;

public enum OperatorTypeEnum implements BaseEnum<String> {
    STAFF("STAFF", "酒店员工"),
    GUEST("GUEST", "顾客"),
    AGENT("AGENT", "智能体");

     private String code;
     private String desc;

     OperatorTypeEnum(String code, String desc){
         this.code = code;
         this.desc = desc;
     }

     public String getCode() {return code; }
     public String getDesc() {return desc; }
}
