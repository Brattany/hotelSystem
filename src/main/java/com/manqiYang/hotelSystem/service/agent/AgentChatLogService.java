package com.manqiYang.hotelSystem.service.agent;

import com.manqiYang.hotelSystem.entity.agent.AgentChatLog;
import java.util.List;

public interface AgentChatLogService {

    AgentChatLog getById(Long agentId);

    List<AgentChatLog> getByOperator(
            String operatorType,
            Long operatorId
    );

    List<AgentChatLog> getAll();

    boolean create(AgentChatLog agentChatLog);

    boolean delete(Long agentChatLogId);
}