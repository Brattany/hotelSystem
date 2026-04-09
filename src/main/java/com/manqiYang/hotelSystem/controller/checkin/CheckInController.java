package com.manqiYang.hotelSystem.controller.checkin;

import com.manqiYang.hotelSystem.common.Result;
import com.manqiYang.hotelSystem.dto.order.CheckInRequest;
import com.manqiYang.hotelSystem.entity.checkin.CheckInRecord;
import com.manqiYang.hotelSystem.service.checkIn.CheckInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/checkIn")
@CrossOrigin(origins = "*")  // 允许所有前端访问
public class CheckInController {

    @Autowired
    private CheckInService checkInService;

    @GetMapping("/{id}")
    public Result<CheckInRecord> getById(@PathVariable Long id) {
        return Result.success(checkInService.getById(id));
    }

    @GetMapping("/reservation")
    public Result<List<CheckInRecord>> getByReservationId(@RequestParam Long reservationId) {
        return Result.success(checkInService.getByReservationId(reservationId));
    }

    @GetMapping("/room")
    public Result<CheckInRecord> getByRoom(@RequestParam String roomNumber) {
        return Result.success(checkInService.getByRoom(roomNumber));
    }

    @GetMapping("/time")
    public Result<List<CheckInRecord>> getByTime(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date checkInTime) {
        return Result.success(checkInService.getByTime(checkInTime));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody CheckInRequest checkInRequest) {
        return Result.success(checkInService.create(checkInRequest));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(checkInService.delete(id));
    }
}
