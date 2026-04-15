package com.manqiYang.hotelSystem.controller.agent;

import com.manqiYang.hotelSystem.common.Result;
import com.manqiYang.hotelSystem.dto.agent.AgentHotelSearchRequest;
import com.manqiYang.hotelSystem.dto.agent.AgentOrderCancelRequest;
import com.manqiYang.hotelSystem.dto.agent.AgentOrderSearchRequest;
import com.manqiYang.hotelSystem.dto.agent.AgentOrderUpdateRequest;
import com.manqiYang.hotelSystem.service.agent.AgentToolService;
import com.manqiYang.hotelSystem.vo.agent.AgentHotelSearchVO;
import com.manqiYang.hotelSystem.vo.agent.AgentOrderDetailVO;
import com.manqiYang.hotelSystem.vo.agent.AgentOrderSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("/agent")
@CrossOrigin(origins = "*")
public class AgentToolController {

    @Autowired
    private AgentToolService agentToolService;

    @PostMapping("/hotels/search")
    public Result<List<AgentHotelSearchVO>> searchHotels(@RequestBody(required = false) AgentHotelSearchRequest request) {
        return execute(() -> agentToolService.searchHotels(request));
    }

    @PostMapping("/orders/search")
    public Result<List<AgentOrderSummaryVO>> searchOrders(@RequestBody(required = false) AgentOrderSearchRequest request) {
        return execute(() -> agentToolService.searchOrders(request));
    }

    @GetMapping("/orders/recent")
    public Result<List<AgentOrderSummaryVO>> getRecentOrders(
            @RequestParam(value = "guestId", required = false) Long guestId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(defaultValue = "5") Integer limit
    ) {
        Long targetGuestId = guestId != null ? guestId : userId;
        return execute(() -> agentToolService.getRecentOrders(targetGuestId, limit));
    }

    @GetMapping("/orders/{reservationId}")
    public Result<AgentOrderDetailVO> getOrderDetail(
            @PathVariable Long reservationId,
            @RequestParam(value = "guestId", required = false) Long guestId,
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        Long targetGuestId = guestId != null ? guestId : userId;
        return execute(() -> agentToolService.getOrderDetail(reservationId, targetGuestId));
    }

    @PutMapping("/orders/{reservationId}")
    public Result<AgentOrderDetailVO> updateOrder(
            @PathVariable Long reservationId,
            @RequestBody AgentOrderUpdateRequest request
    ) {
        return execute(() -> agentToolService.updateOrder(reservationId, request));
    }

    @PostMapping("/orders/{reservationId}/cancel")
    public Result<AgentOrderDetailVO> cancelOrder(
            @PathVariable Long reservationId,
            @RequestBody AgentOrderCancelRequest request
    ) {
        return execute(() -> agentToolService.cancelOrder(reservationId, request));
    }

    private <T> Result<T> execute(Supplier<T> supplier) {
        try {
            return Result.success(supplier.get());
        } catch (RuntimeException exception) {
            return Result.error(exception.getMessage());
        }
    }
}
