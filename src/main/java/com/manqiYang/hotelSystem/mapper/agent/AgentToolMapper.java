package com.manqiYang.hotelSystem.mapper.agent;

import com.manqiYang.hotelSystem.vo.agent.AgentHotelSearchVO;
import com.manqiYang.hotelSystem.vo.agent.AgentOrderDetailVO;
import com.manqiYang.hotelSystem.vo.agent.AgentOrderSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AgentToolMapper {

    List<AgentHotelSearchVO> searchHotels(
            @Param("province") String province,
            @Param("city") String city,
            @Param("district") String district,
            @Param("districtKeyword") String districtKeyword,
            @Param("street") String street,
            @Param("addressKeyword") String addressKeyword,
            @Param("hotelName") String hotelName,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("rating") BigDecimal rating,
            @Param("roomTypeId") Long roomTypeId,
            @Param("roomTypeKeyword") String roomTypeKeyword,
            @Param("facilityMatchMode") String facilityMatchMode,
            @Param("facilityMinMatchCount") Integer facilityMinMatchCount,
            @Param("facilityCount") Integer facilityCount,
            @Param("requireWifi") Integer requireWifi,
            @Param("requireBreakfast") Integer requireBreakfast,
            @Param("requireParking") Integer requireParking,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("roomCount") Integer roomCount
    );

    List<AgentOrderSummaryVO> selectRecentOrders(@Param("guestId") Long guestId, @Param("limit") Integer limit);

    List<AgentOrderSummaryVO> searchOrders(
            @Param("guestId") Long guestId,
            @Param("reservationId") Long reservationId,
            @Param("recentDays") Integer recentDays,
            @Param("province") String province,
            @Param("city") String city,
            @Param("district") String district,
            @Param("hotelName") String hotelName,
            @Param("roomTypeId") Long roomTypeId,
            @Param("roomTypeKeyword") String roomTypeKeyword,
            @Param("status") String status,
            @Param("limit") Integer limit,
            @Param("sort") String sort
    );

    AgentOrderDetailVO selectOrderDetail(@Param("reservationId") Long reservationId);

    Long selectRoomTypeIdByKeyword(@Param("hotelId") Long hotelId, @Param("keyword") String keyword);
}
