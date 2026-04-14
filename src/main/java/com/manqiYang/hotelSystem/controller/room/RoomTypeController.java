package com.manqiYang.hotelSystem.controller.room;

import com.manqiYang.hotelSystem.common.Result;
import com.manqiYang.hotelSystem.dto.room.AvailableRoomTypeRequest;
import com.manqiYang.hotelSystem.dto.room.UpdateRoomTypeRequest;
import com.manqiYang.hotelSystem.entity.room.RoomType;
import com.manqiYang.hotelSystem.service.room.RoomTypeService;
import com.manqiYang.hotelSystem.vo.RoomTypeVO;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roomType")
@CrossOrigin(origins = "*")  // 允许所有前端访问
public class RoomTypeController {

    @Autowired
    private RoomTypeService roomTypeService;

    @GetMapping("/hotel")
    public Result<List<RoomType>> getByHotelId(@RequestParam Long hotelId) {
        return Result.success(roomTypeService.getByHotelId(hotelId));
    }

    @GetMapping("/name")
    public Result<List<RoomType>> getByName(@RequestParam String name) {
        return Result.success(roomTypeService.getByName(name));
    }

    @PostMapping("/available")
    public Result<List<RoomTypeVO>> getAvailableTypes(@RequestBody AvailableRoomTypeRequest availableRoomTypeRequest) {
        return Result.success(roomTypeService.getAvailableRoomTypes(availableRoomTypeRequest));
    }

    @GetMapping("/price")
    public Result<List<RoomType>> getByPrice(@RequestParam Integer price) {
        return Result.success(roomTypeService.getByPrice(price));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody RoomType roomType) {
        return Result.success(roomTypeService.create(roomType));
    }

    @PutMapping("/update")
    public Result<Integer> updateInfo(@RequestBody UpdateRoomTypeRequest updateRoomTypeRequest) {
        return Result.success(roomTypeService.update(updateRoomTypeRequest));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(roomTypeService.delete(id));
    }
}
