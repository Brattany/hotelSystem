package com.manqiYang.hotelSystem.entity.agent;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentKnowledge {

    private Long knowledgeId;

    private Long agentId;

    /**
     * 知识类型：POLICY / ACTION / TEMPLATE / FAQ
     */
    private String knowledgeType;

    private String knowledgeKey;

    private String knowledgeValue;

    private String applicableScope;

    private Integer priority;

    private Integer versions;

    /**
     * 状态：1-启用 0-停用
     */
    private Integer status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
