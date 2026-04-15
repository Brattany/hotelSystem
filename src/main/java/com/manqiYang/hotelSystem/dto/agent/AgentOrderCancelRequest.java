package com.manqiYang.hotelSystem.dto.agent;

import lombok.Data;

@Data
public class AgentOrderCancelRequest {

    private Long guestId;

    /**
     * 预留给智能体使用，当前不落库，仅用于响应说明。
     */
    private String reason;
}
