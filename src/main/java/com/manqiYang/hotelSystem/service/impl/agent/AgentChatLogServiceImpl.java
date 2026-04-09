package com.manqiYang.hotelSystem.service.impl.agent;

import com.manqiYang.hotelSystem.entity.agent.AgentChatLog;
import com.manqiYang.hotelSystem.mapper.agent.AgentChatLogMapper;
import com.manqiYang.hotelSystem.service.agent.AgentChatLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentChatLogServiceImpl implements AgentChatLogService {

    @Autowired
    public AgentChatLogMapper aclMapper;

    @Override
    public AgentChatLog getById(Long agentId){
        return aclMapper.selectByAgentId(agentId);
    }

    @Override
    public List<AgentChatLog> getByOperator(String operatorType, Long operatorId){
        return aclMapper.selectByOperator(operatorType,operatorId);
    }

    @Override
    public List<AgentChatLog> getAll(){
        return aclMapper.selectAll();
    }

    @Override
    public boolean create(AgentChatLog agentChatLog){
        return aclMapper.insert(agentChatLog);
    }

    @Override
    public boolean delete(Long id){return aclMapper.deleteById(id);}
}
