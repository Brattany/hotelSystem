package com.manqiYang.hotelSystem.mapper.agent;

import com.manqiYang.hotelSystem.entity.agent.Agent;
import com.manqiYang.hotelSystem.mapper.base.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentMapper extends BaseMapper<Agent, Long> {

    Agent selectByCode(String agentCode);

    List<Agent> selectByName(String agentName);

    List<Agent> selectByHotelId(Long hotelId);

    List<Agent> selectByStatus(Integer status);

    List<Agent> selectAll();

    int deleteByCode(String code);
}
