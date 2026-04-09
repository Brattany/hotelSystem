package com.manqiYang.hotelSystem.controller.reservation;

import com.manqiYang.hotelSystem.common.Result;
import com.manqiYang.hotelSystem.entity.reservation.Reservation;
import com.manqiYang.hotelSystem.entity.room.RoomType;
import com.manqiYang.hotelSystem.enums.reservation.ReservationEnum;
import com.manqiYang.hotelSystem.service.reservation.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/reservation")
@CrossOrigin(origins = "*")  // 允许所有前端访问
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @GetMapping("/{id}")
    public Result<Reservation> getById(@PathVariable Long id) {
        return Result.success(reservationService.getById(id));
    }

    @GetMapping("/getByHotelId")
    public Result<List<Reservation>> getByHotelId(@RequestParam Long hotelId) {
        return Result.success(reservationService.getByHotelId(hotelId));
    }

    @GetMapping("/status")
    public Result<List<Reservation>> getByStatus(@RequestParam Long hotelId, @RequestParam Integer status) {
        return Result.success(reservationService.getByStatus(hotelId, status));
    }

    @GetMapping("/range")
    public Result<List<Reservation>> getByDateRange(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endDate) {
        return Result.success(reservationService.getByDateRange(hotelId, startDate, endDate));
    }

    @GetMapping("/type")
    public Result<String> getRoomType(@RequestParam Long id) {
        return Result.success(reservationService.getRoomType(id));
    }

    @PostMapping("/newReservation")
    public Result<Boolean> create(@RequestBody Reservation reservation) {
        return Result.success(reservationService.create(reservation));
    }

    @PutMapping("/upstateRoomType")
    public Result<Integer> updateRoomType(@RequestParam Long id, @RequestParam Long typeId) {
        RoomType roomType = new RoomType();
        roomType.setTypeId(typeId);
        return Result.success(reservationService.updateRoomType(id, roomType));
    }

    @PutMapping("/upstateCheckInDate")
    public Result<Integer> updateCheckInDate(
            @RequestParam Long id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date checkInDate) {
        return Result.success(reservationService.updateCheckInDate(id, checkInDate));
    }

    @PutMapping("/upstateCheckOutDate")
    public Result<Integer> updateCheckOutDate(
            @RequestParam Long id,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date checkOutDate) {
        return Result.success(reservationService.updateCheckOutDate(id, checkOutDate));
    }

    @PutMapping("/upstateStatus")
    public Result<Integer> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        return Result.success(reservationService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(reservationService.delete(id));
    }

    @GetMapping("/guest/phone")
    public Result<List<Reservation>> getByGuestPhone(@RequestParam String phone) {
        return Result.success(reservationService.getByGuestPhone(phone));
    }
}
