package com.manqiYang.hotelSystem.controller.log;

import com.manqiYang.hotelSystem.common.Result;
import com.manqiYang.hotelSystem.entity.log.OrderChangeLog;
import com.manqiYang.hotelSystem.service.log.OrderChangeLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orderChangeLog")
@CrossOrigin(origins = "*")  // 允许所有前端访问
public class OrderChangeLogController {

    @Autowired
    private OrderChangeLogService orderChangeLogService;

    @GetMapping
    public Result<List<OrderChangeLog>> getAll() {
        return Result.success(orderChangeLogService.getAll());
    }

    @GetMapping("/{id}")
    public Result<OrderChangeLog> getById(@PathVariable Long id) {
        return Result.success(orderChangeLogService.getById(id));
    }

    @GetMapping("/changeType/{changeType}")
    public Result<List<OrderChangeLog>> getByChangeType(@PathVariable String changeType) {
        return Result.success(orderChangeLogService.getByCType(changeType));
    }

    @GetMapping("/operatorType/{operatorType}")
    public Result<List<OrderChangeLog>> getByOperatorType(@PathVariable String operatorType) {
        return Result.success(orderChangeLogService.getByOType(operatorType));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody OrderChangeLog orderChangeLog) {
        return Result.success(orderChangeLogService.create(orderChangeLog));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(orderChangeLogService.delete(id));
    }
}
