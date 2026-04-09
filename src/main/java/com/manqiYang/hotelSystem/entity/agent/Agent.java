package com.manqiYang.hotelSystem.entity.agent;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Agent {

    private Long agentId;

    /**
     * 智能体编码（系统内部唯一标识）
     */
    private String agentCode;

    /**
     * 智能体名称
     */
    private String agentName;

    /**
     * 状态：1-启用，0-停用
     */
    private Integer status;

    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
