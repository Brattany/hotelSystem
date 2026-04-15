package com.manqiYang.hotelSystem.service.agent;

import com.manqiYang.hotelSystem.dto.agent.AgentHotelSearchRequest;
import com.manqiYang.hotelSystem.dto.agent.AgentOrderCancelRequest;
import com.manqiYang.hotelSystem.dto.agent.AgentOrderSearchRequest;
import com.manqiYang.hotelSystem.dto.agent.AgentOrderUpdateRequest;
import com.manqiYang.hotelSystem.vo.agent.AgentHotelSearchVO;
import com.manqiYang.hotelSystem.vo.agent.AgentOrderDetailVO;
import com.manqiYang.hotelSystem.vo.agent.AgentOrderSummaryVO;

import java.util.List;

public interface AgentToolService {

    List<AgentHotelSearchVO> searchHotels(AgentHotelSearchRequest request);

    List<AgentOrderSummaryVO> getRecentOrders(Long guestId, Integer limit);

    List<AgentOrderSummaryVO> searchOrders(AgentOrderSearchRequest request);

    AgentOrderDetailVO getOrderDetail(Long reservationId, Long guestId);

    AgentOrderDetailVO updateOrder(Long reservationId, AgentOrderUpdateRequest request);

    AgentOrderDetailVO cancelOrder(Long reservationId, AgentOrderCancelRequest request);
}
