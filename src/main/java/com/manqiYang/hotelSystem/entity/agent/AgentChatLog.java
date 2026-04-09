package com.manqiYang.hotelSystem.entity.agent;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AgentChatLog {

    private Long chatId;

    private String sessionId;

    private Integer turnIndex;

    private Long agentId;

    private Long guestId;

    private String userInput;

    private String agentOutput;

    /**
     * 意图类型：RECOMMEND / POLICY / ORDER
     */
    private String intentType;

    private String city;

    private BigDecimal priceMin;

    private BigDecimal priceMax;

    private String tag;

    private Long hotelId;

    private Long reservationId;

    /**
     * 操作类型（用于订单）：CANCEL / MODIFY / QUERY
     */
    private String actionType;

    /**
     * 状态（是否成功）：1-成功 0-失败
     */
    private Integer actionStatus;

    private LocalDateTime createTime;
}