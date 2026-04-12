package com.manqiYang.hotelSystem.controller.guest;

import com.manqiYang.hotelSystem.common.Result;
import com.manqiYang.hotelSystem.dto.guest.LoginRequest;
import com.manqiYang.hotelSystem.dto.guest.RegisterRequest;
import com.manqiYang.hotelSystem.entity.guest.Guest;
import com.manqiYang.hotelSystem.service.guest.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/guest")
@CrossOrigin(origins = "*")  // 允许所有前端访问
public class GuestController {

    @Autowired
    private GuestService guestService;

    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody RegisterRequest registerRequest) {
        return Result.success(guestService.register(registerRequest));
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginRequest loginRequest) {
        return Result.success(guestService.login(loginRequest));
    }

    @GetMapping("/wxLogin")
    public Result<String> getMethodName(@RequestParam String code) {
        String result = guestService.wxLogin(code);
        
        if (result != null && result.startsWith("UNBOUND:")) {
            return Result.success(result); 
        }
        
        return Result.success(result);
    }
    

    @GetMapping("/code")
    public Result<String> sendcode(@RequestParam String phone){
        return Result.success(guestService.sendCode(phone));
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
