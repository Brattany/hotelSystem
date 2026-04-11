package com.manqiYang.hotelSystem.controller.checkin;

import com.manqiYang.hotelSystem.common.Result;
import com.manqiYang.hotelSystem.entity.checkin.CheckOutRecord;
import com.manqiYang.hotelSystem.service.checkIn.CheckOutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/checkOut")
@CrossOrigin(origins = "*")  // 允许所有前端访问
public class CheckOutController {

    @Autowired
    private CheckOutService checkOutService;

    @GetMapping("/all")
    public Result<List<CheckOutRecord>> getAll() {
        return Result.success(checkOutService.getAll());
    }

    @GetMapping("/{id}")
    public Result<CheckOutRecord> getById(@PathVariable Long id) {
        return Result.success(checkOutService.getById(id));
    }

    @GetMapping("/checkIn/{checkInId}")
    public Result<CheckOutRecord> getByCheckInId(@PathVariable Long checkInId) {
        return Result.success(checkOutService.getByCheckInId(checkInId));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestParam String roomNumber) {
        return Result.success(checkOutService.create(roomNumber));
    }

    @DeleteMapping("/delete")
    public Result<Boolean> delete(@RequestParam Long id) {
        return Result.success(checkOutService.delete(id));
    }
}
