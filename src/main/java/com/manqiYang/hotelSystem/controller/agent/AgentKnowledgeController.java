package com.manqiYang.hotelSystem.controller.agent;

import com.manqiYang.hotelSystem.common.Result;
import com.manqiYang.hotelSystem.entity.agent.AgentKnowledge;
import com.manqiYang.hotelSystem.enums.agent.KnowledgeTypeEnum;
import com.manqiYang.hotelSystem.service.agent.AgentKnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agentKnowledge")
@CrossOrigin(origins = "*")  // 允许所有前端访问
public class AgentKnowledgeController {

    @Autowired
    private AgentKnowledgeService agentKnowledgeService;

    @GetMapping
    public Result<List<AgentKnowledge>> getAll() {
        return Result.success(agentKnowledgeService.getAll());
    }

    @GetMapping("/agent/{agentId}")
    public Result<List<AgentKnowledge>> getByAgentId(@PathVariable Long agentId) {
        return Result.success(agentKnowledgeService.getByAgentId(agentId));
    }

    @GetMapping("/type")
    public Result<List<AgentKnowledge>> getByType(@RequestParam KnowledgeTypeEnum type) {
        return Result.success(agentKnowledgeService.getByType(type));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody AgentKnowledge knowledge) {
        return Result.success(agentKnowledgeService.create(knowledge));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody AgentKnowledge knowledge) {
        return Result.success(agentKnowledgeService.update(knowledge));
    }

    @DeleteMapping("/{knowledgeId}")
    public Result<Boolean> delete(@PathVariable Long knowledgeId) {
        return Result.success(agentKnowledgeService.delete(knowledgeId));
    }
}
