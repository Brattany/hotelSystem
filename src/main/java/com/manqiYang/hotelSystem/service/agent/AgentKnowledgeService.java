package com.manqiYang.hotelSystem.service.agent;

import com.manqiYang.hotelSystem.entity.agent.AgentKnowledge;
import com.manqiYang.hotelSystem.enums.agent.KnowledgeTypeEnum;

import java.util.List;

public interface AgentKnowledgeService {
    List<AgentKnowledge> getAll();

    List<AgentKnowledge> getByAgentId(Long agentId);

    List<AgentKnowledge> getByType(KnowledgeTypeEnum type);

    boolean create(AgentKnowledge knowledge);

    boolean update(AgentKnowledge knowledge);

    boolean delete(Long knowledgeID);
}
