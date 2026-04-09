package com.manqiYang.hotelSystem.service.agent;

import com.manqiYang.hotelSystem.entity.agent.Agent;
import com.manqiYang.hotelSystem.entity.hotel.Hotel;
import com.manqiYang.hotelSystem.entity.reservation.Reservation;

import java.math.BigDecimal;
import java.util.List;

public interface AgentService {
    boolean create(Agent agent);

    Agent getByCode(String code);

    List<Agent> getByName(String name);

    List<Agent> getByHotelId(Long hotelId);

    List<Agent> getByStatus(Integer status);

    List<Agent> getAll();

    boolean delete(String code);

    // 查询符合条件的酒店
    List<Hotel> searchHotels(String city, BigDecimal minPrice, BigDecimal maxPrice, String tag, Integer hasWifi, Integer hasBreakfast, Integer hasParking);

    // 查询酒店政策
    String getHotelPolicy(Long hotelId);

    // 操作订单
    boolean cancelReservation(Long reservationId, Long guestId);

    boolean modifyReservation(Long reservationId, Integer roomCount, String checkInDate, String checkOutDate);

    // 查询订单
    List<Reservation> getReservationsByGuest(Long guestId);

    // 提示订单情况
    List<Reservation> getOverdueReservations();
}
