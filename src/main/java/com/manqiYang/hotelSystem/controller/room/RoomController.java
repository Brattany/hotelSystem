package com.manqiYang.hotelSystem.controller.room;

import com.manqiYang.hotelSystem.common.Result;
import com.manqiYang.hotelSystem.dto.room.GetByTSRequest;
import com.manqiYang.hotelSystem.dto.room.GetRoomsRequest;
import com.manqiYang.hotelSystem.dto.room.UpdateInfoRequest;
import com.manqiYang.hotelSystem.dto.room.UpdateStatusRequest;
import com.manqiYang.hotelSystem.entity.room.Room;
import com.manqiYang.hotelSystem.service.room.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/room")
@CrossOrigin(origins = "*")  // 允许所有前端访问
public class RoomController {

    @Autowired
    private RoomService roomService;

    @GetMapping("/roomNumber")
    public Result<Room> roomInfo(@RequestParam String roomNumber, @RequestParam Long hotelId){
        return Result.success(roomService.getByRoomNumber(roomNumber, hotelId));
    }

    @GetMapping("/hotel")
    public Result<List<Room>> getByHotel(@RequestParam Long hotelId){
        return Result.success(roomService.getByHotel(hotelId));
    }

    @PostMapping("/typeAndStatus")
    public Result<List<Room>> getByTAndS(@RequestBody GetByTSRequest getByTSRequest){
        return Result.success(roomService.getByTAndS(getByTSRequest));
    }

    @PostMapping("/filter")
    public Result<List<Room>> getByStatus(@RequestBody GetRoomsRequest getRoomsRequest){
        return Result.success(roomService.getByStatus(getRoomsRequest));
    }

    @GetMapping("/typeId")
    public Result<List<Room>> getByType(@RequestParam Long typeId){
        return Result.success(roomService.getByType(typeId));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody Room room){
        return Result.success(roomService.create(room));
    }

    @PutMapping("/updateStatus")
    public Result<Boolean> updateStatus(@RequestBody UpdateStatusRequest updateStatusRequest){
        return Result.success(roomService.updateStatus(updateStatusRequest.getRoomId(),updateStatusRequest.getStatus()));
    }

    @PutMapping("/updateInfo")
    public Result<Boolean> update(@RequestBody UpdateInfoRequest updateInfoRequest){
        return Result.success(roomService.updateInfo(updateInfoRequest));
    }

    @DeleteMapping("/delete")
    public Result<Boolean> delete(@RequestParam Long roomId){
        return Result.success(roomService.delete(roomId));
    }
}
