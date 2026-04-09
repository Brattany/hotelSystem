package com.manqiYang.hotelSystem.controller.guest;

import com.manqiYang.hotelSystem.common.Result;
import com.manqiYang.hotelSystem.entity.guest.Guest;
import com.manqiYang.hotelSystem.service.guest.GuestService;
import com.manqiYang.hotelSystem.util.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/guest")
@CrossOrigin(origins = "*")  // 允许所有前端访问
public class GuestController {

    @Autowired
    private GuestService guestService;

    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody Guest guest) {
        return Result.success(guestService.register(guest));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam String phone) {
        Guest guest = guestService.getByPhone(phone);
        if (guest == null) {
            throw new RuntimeException("用户不存在");
        }
        String token = JwtUtil.createToken(guest.getGuestId(), guest.getPhone(), "GUEST");
        Map<String, Object> result = Map.of("token", token, "user", guest);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<Guest> getById(@PathVariable Long id) {
        return Result.success(guestService.getById(id));
    }

    @GetMapping("/phone")
    public Result<Guest> getByPhone(@RequestParam String phone) {
        return Result.success(guestService.getByPhone(phone));
    }

    @GetMapping("/openId/{openId}")
    public Result<Guest> getByOpenId(@PathVariable String openId) {
        return Result.success(guestService.getByOpenId(openId));
    }

    @PostMapping("/create")
    public Result<Boolean> create(@RequestBody Guest guest) {
        return Result.success(guestService.create(guest));
    }

    @PutMapping("/update")
    public Result<Boolean> update(@RequestBody Guest guest) {
        return Result.success(guestService.update(guest));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(guestService.delete(id));
    }
}
