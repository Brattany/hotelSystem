package com.manqiYang.hotelSystem.service.impl.agent;

import com.manqiYang.hotelSystem.entity.agent.AgentKnowledge;
import com.manqiYang.hotelSystem.enums.agent.KnowledgeTypeEnum;
import com.manqiYang.hotelSystem.mapper.agent.AgentKnowledgeMapper;
import com.manqiYang.hotelSystem.service.agent.AgentKnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentKnowledgeServiceImpl implements AgentKnowledgeService {

    @Autowired
    public AgentKnowledgeMapper akMapper;

    @Override
    public List<AgentKnowledge> getAll(){return akMapper.selectAll();}

    @Override
    public List<AgentKnowledge> getByAgentId(Long agentId){
        return akMapper.selectByAgentId(agentId);
    }

    @Override
    public List<AgentKnowledge> getByType(KnowledgeTypeEnum type)
    {
        return akMapper.selectByType(type);
    }

    @Override
    public boolean create(AgentKnowledge knowledge)
    {
        return akMapper.insert(knowledge);
    }

    @Override
    public boolean update(AgentKnowledge knowledge)
    {
        return akMapper.updateById(knowledge);
    }

    @Override
    public boolean delete(Long knowledgeId){return akMapper.deleteById(knowledgeId);}
}
