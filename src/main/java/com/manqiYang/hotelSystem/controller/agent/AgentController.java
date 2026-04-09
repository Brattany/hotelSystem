package com.manqiYang.hotelSystem.controller.agent;

import com.manqiYang.hotelSystem.common.Result;
import com.manqiYang.hotelSystem.entity.agent.Agent;
import com.manqiYang.hotelSystem.entity.hotel.Hotel;
import com.manqiYang.hotelSystem.entity.reservation.Reservation;
import com.manqiYang.hotelSystem.service.agent.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/agent")
@CrossOrigin(origins = "*")  // 允许所有前端访问
public class AgentController {

    @Autowired
    private AgentService agentService;

    @GetMapping
    public Result<List<Agent>> getAll() {
        return Result.success(agentService.getAll());
    }

    @GetMapping("/getByCode")
    public Result<Agent> getByCode(@RequestParam String code) {
        return Result.success(agentService.getByCode(code));
    }

    @GetMapping("/getByName")
    public Result<List<Agent>> getByName(@RequestParam String name) {
        return Result.success(agentService.getByName(name));
    }

    @GetMapping("/getByHotel")
    public Result<List<Agent>> getByHotelId(@RequestParam Long hotelId) {
        return Result.success(agentService.getByHotelId(hotelId));
    }

    @GetMapping("/getByStatus")
    public Result<List<Agent>> getByStatus(@RequestParam Integer status) {
        return Result.success(agentService.getByStatus(status));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody Agent agent) {
        return Result.success(agentService.create(agent));
    }

    @DeleteMapping("/deleteByCode")
    public Result<Boolean> delete(@RequestParam String code) {
        return Result.success(agentService.delete(code));
    }

    // 搜索酒店
    @GetMapping("/search/hotels")
    public Result<List<Hotel>> searchHotels(@RequestParam(required = false) String city,
                                            @RequestParam(required = false) BigDecimal minPrice,
                                            @RequestParam(required = false) BigDecimal maxPrice,
                                            @RequestParam(required = false) String tag,
                                            @RequestParam(required = false) Integer hasWifi,
                                            @RequestParam(required = false) Integer hasBreakfast,
                                            @RequestParam(required = false) Integer hasParking) {
        return Result.success(agentService.searchHotels(city, minPrice, maxPrice, tag, hasWifi, hasBreakfast, hasParking));
    }

    // 获取酒店政策
    @GetMapping("/hotel/{hotelId}/policy")
    public Result<String> getHotelPolicy(@PathVariable Long hotelId) {
        return Result.success(agentService.getHotelPolicy(hotelId));
    }

    // 取消订单
    @PostMapping("/reservation/{reservationId}/cancel")
    public Result<Boolean> cancelReservation(@PathVariable Long reservationId, @RequestParam Long guestId) {
        return Result.success(agentService.cancelReservation(reservationId, guestId));
    }

    // 修改订单
    @PutMapping("/reservation/{reservationId}/modify")
    public Result<Boolean> modifyReservation(@PathVariable Long reservationId,
                                             @RequestParam(required = false) Integer roomCount,
                                             @RequestParam(required = false) String checkInDate,
                                             @RequestParam(required = false) String checkOutDate) {
        return Result.success(agentService.modifyReservation(reservationId, roomCount, checkInDate, checkOutDate));
    }

    // 查询用户订单
    @GetMapping("/reservations/guest/{guestId}")
    public Result<List<Reservation>> getReservationsByGuest(@PathVariable Long guestId) {
        return Result.success(agentService.getReservationsByGuest(guestId));
    }

    // 获取逾期订单
    @GetMapping("/reservations/overdue")
    public Result<List<Reservation>> getOverdueReservations() {
        return Result.success(agentService.getOverdueReservations());
    }
}
