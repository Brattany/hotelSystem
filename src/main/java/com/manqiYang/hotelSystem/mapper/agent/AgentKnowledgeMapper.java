package com.manqiYang.hotelSystem.mapper.agent;

import com.manqiYang.hotelSystem.entity.agent.AgentKnowledge;
import com.manqiYang.hotelSystem.enums.agent.KnowledgeTypeEnum;
import com.manqiYang.hotelSystem.mapper.base.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentKnowledgeMapper extends BaseMapper<AgentKnowledge, Long> {
    List<AgentKnowledge> selectAll();

    List<AgentKnowledge> selectByAgentId(Long agentId);

    List<AgentKnowledge> selectByType(KnowledgeTypeEnum type);
}

