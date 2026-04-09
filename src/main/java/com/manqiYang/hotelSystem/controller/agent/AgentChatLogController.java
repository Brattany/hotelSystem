package com.manqiYang.hotelSystem.controller.agent;

import com.manqiYang.hotelSystem.common.Result;
import com.manqiYang.hotelSystem.entity.agent.AgentChatLog;
import com.manqiYang.hotelSystem.service.agent.AgentChatLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agentChatLog")
@CrossOrigin(origins = "*")  // 允许所有前端访问
public class AgentChatLogController {

    @Autowired
    private AgentChatLogService agentChatLogService;

    @GetMapping
    public Result<List<AgentChatLog>> getAll() {
        return Result.success(agentChatLogService.getAll());
    }

    @GetMapping("/agent")
    public Result<AgentChatLog> getByAgentId(@RequestParam Long agentId) {
        return Result.success(agentChatLogService.getById(agentId));
    }

    @GetMapping("/operator")
    public Result<List<AgentChatLog>> getByOperator(@RequestParam String operatorType,
                                                    @RequestParam Long operatorId) {
        return Result.success(agentChatLogService.getByOperator(operatorType, operatorId));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody AgentChatLog agentChatLog) {
        return Result.success(agentChatLogService.create(agentChatLog));
    }

    @DeleteMapping("/delete")
    public Result<Boolean> delete(@RequestParam Long id) {
        return Result.success(agentChatLogService.delete(id));
    }
}
