package com.manqiYang.hotelSystem.controller.user;

import com.manqiYang.hotelSystem.common.Result;
import com.manqiYang.hotelSystem.dto.user.LoginByCodeRequest;
import com.manqiYang.hotelSystem.dto.user.LoginByPasswordRequest;
import com.manqiYang.hotelSystem.dto.user.PasswordChangeRequest;
import com.manqiYang.hotelSystem.dto.user.RegisterRequest;
import com.manqiYang.hotelSystem.entity.user.SysUser;
import com.manqiYang.hotelSystem.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*")  // 允许所有前端访问
public class UserController {

    @Autowired
    private UserService userService;

    // REST style APIs
    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest request){
        userService.register(request);
        return Result.success();
    }

    @PostMapping("/login/password")
    public Result<String> login(@RequestBody LoginByPasswordRequest request) {
        return Result.success(userService.login(request));
    }

    @PostMapping("/login/code")
    public Result<String> loginByPhone(@RequestBody LoginByCodeRequest request){
        return Result.success(userService.loginByPhone(request));
    }

    @GetMapping("/code")
    public Result<String> sendcode(@RequestParam String phone){
        return Result.success(userService.sendCode(phone));
    }

    @GetMapping("/info")
    public Result<SysUser> getInfo(@RequestParam String phone) {
        return Result.success(userService.getByPhone(phone));
    }

    @GetMapping("/hotel/{hotelId}")
    public Result<java.util.List<SysUser>> getByHotelId(@PathVariable Long hotelId) {
        return Result.success(userService.getByHotelId(hotelId));
    }

    @GetMapping("/status")
    public Result<java.util.List<SysUser>> getByStatus(@RequestParam Integer status) {
        return Result.success(userService.getByStatus(status));
    }

    @GetMapping("/role")
    public Result<java.util.List<SysUser>> getByRole(@RequestParam String role) {
        return Result.success(userService.getByRole(role));
    }

    @PutMapping("/name")
    public Result<Boolean> renameRest(@RequestParam String newName){
        return Result.success(userService.rename(newName));
    }

    @PutMapping("/password")
    public Result<Boolean> passwordChangeRest(@RequestBody PasswordChangeRequest passwordChangeRequest){
        return Result.success(userService.passwordChange(passwordChangeRequest.getOldPass(), passwordChangeRequest.getNewPass()));
    }

    @PutMapping("/phone")
    public Result<Boolean> phoneChangeRest(@RequestParam String newPhone){
        return Result.success(userService.phoneChange(newPhone));
    }

    @PutMapping("/status")
    public Result<Boolean> statusChangeRest(@RequestParam Integer status){
        return Result.success(userService.statusChange(status));
    }

    @PutMapping("/role")
    public Result<Boolean> updateRole(@RequestParam String role){
        return Result.success(userService.updateRole(role));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteRest(@PathVariable Long id){
        return Result.success(userService.delete(id));
    }

    // Legacy APIs kept for compatibility
    @PostMapping("/rename")
    public Result<Boolean> rename(@RequestParam String newName){
        return Result.success(userService.rename(newName));
    }

    @PostMapping("/passwordChange")
    public Result<Boolean> passwordChange(@RequestBody PasswordChangeRequest passwordChangeRequest){
        return Result.success(userService.passwordChange(passwordChangeRequest.getOldPass(), passwordChangeRequest.getNewPass()));
    }

    @PostMapping("/phoneChange")
    public Result<Boolean> phoneChange(@RequestParam String newPhone){
        return Result.success(userService.phoneChange(newPhone));
    }

    @PostMapping("/statusChange")
    public Result<Boolean> statusChange(@RequestParam Integer status){
        return Result.success(userService.statusChange(status));
    }

    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestParam Long id){
        return Result.success(userService.delete(id));
    }
}