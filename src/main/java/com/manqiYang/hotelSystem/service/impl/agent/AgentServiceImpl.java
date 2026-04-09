package com.manqiYang.hotelSystem.service.impl.agent;

import com.manqiYang.hotelSystem.entity.agent.Agent;
import com.manqiYang.hotelSystem.entity.hotel.Hotel;
import com.manqiYang.hotelSystem.entity.reservation.Reservation;
import com.manqiYang.hotelSystem.mapper.agent.AgentMapper;
import com.manqiYang.hotelSystem.mapper.hotel.HotelMapper;
import com.manqiYang.hotelSystem.mapper.reservation.ReservationMapper;
import com.manqiYang.hotelSystem.service.agent.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl implements AgentService {

    @Autowired
    public AgentMapper agentMapper;

    @Autowired
    private HotelMapper hotelMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Override
    public Agent getByCode(String code){
        return agentMapper.selectByCode(code);
    }

    @Override
    public List<Agent> getByName(String name){
        return agentMapper.selectByName(name);
    }

    @Override
    public List<Agent> getByHotelId(Long hotelId){
        return agentMapper.selectByHotelId(hotelId);
    }

    @Override
    public List<Agent> getByStatus(Integer status){
        return agentMapper.selectByStatus(status);
    }

    @Override
    public List<Agent> getAll(){
        return agentMapper.selectAll();
    }

    @Override
    public boolean create(Agent agent){
        agent.setIsDelete(0);
        return agentMapper.insert(agent);
    }

    @Override
    public boolean delete(String code){
        return agentMapper.deleteByCode(code) > 0;
    }

    @Override
    public List<Hotel> searchHotels(String city, BigDecimal minPrice, BigDecimal maxPrice, String tag, Integer hasWifi, Integer hasBreakfast, Integer hasParking) {
        // 简单实现，实际可优化为动态查询
        List<Hotel> hotels = hotelMapper.selectAll();
        return hotels.stream()
                .filter(h -> city == null || city.equals(h.getCity()))
                .filter(h -> minPrice == null || h.getPriceMin().compareTo(minPrice) >= 0)
                .filter(h -> maxPrice == null || h.getPriceMax().compareTo(maxPrice) <= 0)
                .filter(h -> hasWifi == null || hasWifi.equals(h.getHasWifi()))
                .filter(h -> hasBreakfast == null || hasBreakfast.equals(h.getHasBreakfast()))
                .filter(h -> hasParking == null || hasParking.equals(h.getHasParking()))
                .collect(Collectors.toList());
    }

    @Override
    public String getHotelPolicy(Long hotelId) {
        Hotel hotel = hotelMapper.selectById(hotelId);
        return hotel != null ? hotel.getCancelPolicy() : null;
    }

    @Override
    public boolean cancelReservation(Long reservationId, Long guestId) {
        Reservation res = reservationMapper.selectById(reservationId);
        if (res != null && res.getGuestId().equals(guestId)) {
            res.setStatus(3); // canceled
            return reservationMapper.updateById(res);
        }
        return false;
    }

    @Override
    public boolean modifyReservation(Long reservationId, Integer roomCount, String checkInDate, String checkOutDate) {
        Reservation res = reservationMapper.selectById(reservationId);
        if (res != null) {
            if (roomCount != null) res.setRoomCount(roomCount);
            if (checkInDate != null) res.setCheckInDate(LocalDate.parse(checkInDate));
            if (checkOutDate != null) res.setCheckOutDate(LocalDate.parse(checkOutDate));
            return reservationMapper.updateById(res);
        }
        return false;
    }

    @Override
    public List<Reservation> getReservationsByGuest(Long guestId) {
        return reservationMapper.selectByGuestId(guestId);
    }

    @Override
    public List<Reservation> getOverdueReservations() {
        // 简单实现，查询check_out_date < today and status = 4 (checked_in)
        List<Reservation> reservations = reservationMapper.selectAll();
        LocalDate today = LocalDate.now();
        return reservations.stream()
                .filter(r -> r.getStatus() == 4 && r.getCheckOutDate().isBefore(today))
                .collect(Collectors.toList());
    }
}
