package com.manqiYang.hotelSystem.mapper.agent;

import com.manqiYang.hotelSystem.entity.agent.AgentChatLog;
import com.manqiYang.hotelSystem.mapper.base.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentChatLogMapper extends BaseMapper<AgentChatLog, Long> {
    List<AgentChatLog> selectAll();

    AgentChatLog selectByAgentId(Long agentId);

    List<AgentChatLog> selectByOperator(
            String operatorType,
            Long operatorId
    );
}

