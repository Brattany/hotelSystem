package com.manqiYang.hotelSystem.controller.user;

import com.manqiYang.hotelSystem.common.Result;
import com.manqiYang.hotelSystem.dto.user.LoginByCodeRequest;
import com.manqiYang.hotelSystem.dto.user.LoginByPasswordRequest;
import com.manqiYang.hotelSystem.dto.user.PasswordChangeRequest;
import com.manqiYang.hotelSystem.dto.user.RegisterRequest;
import com.manqiYang.hotelSystem.entity.user.SysUser;
import com.manqiYang.hotelSystem.service.user.UserService;
import com.manqiYang.hotelSystem.util.jwt.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*")  // 允许所有前端访问
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

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

    @GetMapping("infoById")
    public Result<SysUser> getInfoById(@RequestParam Long userId) {
        return Result.success(userService.getById(userId));
    }
    

    @GetMapping("/info")
    public Result<SysUser> getInfo(@RequestParam String phone) {
        return Result.success(userService.getByPhone(phone));
    }

    //修改用户名
    @PutMapping("/name")
    public Result<Boolean> renameRest(@RequestParam Long userId, @RequestParam String newName){
        return Result.success(userService.rename(userId, newName));
    }

    //修改密码
    @PutMapping("/password")
    public Result<Boolean> passwordChangeRest(@RequestBody PasswordChangeRequest passwordChangeRequest){
        return Result.success(userService.passwordChange(passwordChangeRequest.getUserId(), passwordChangeRequest.getOldPass(), passwordChangeRequest.getNewPass()));
    }

    //修改电话号码
    @PutMapping("/phone")
    public Result<Boolean> phoneChangeRest(@RequestParam Long userId, @RequestParam String newPhone){
        return Result.success(userService.phoneChange(userId, newPhone));
    }

    //更改状态
    @PutMapping("/status")
    public Result<Boolean> statusChangeRest(@RequestParam Long userId, @RequestParam Integer status){
        return Result.success(userService.statusChange(userId, status));
    }

    //更改职务
    @PutMapping("/role")
    public Result<Boolean> updateRole(@RequestParam Long userId, @RequestParam String role){
        return Result.success(userService.updateRole(userId, role));
    }

    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestParam Long id){
        return Result.success(userService.delete(id));
    }

    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); 
        } else {
            return Result.success("已退出");
        }

        try {
            long expiration = JwtUtil.getExpirationDate(token).getTime();
            long now = System.currentTimeMillis();
            long ttl = expiration - now;

            if (ttl > 0) {
                redisTemplate.opsForValue().set("jwt_blacklist:" + token, "logout", ttl, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            ;
        }

        return Result.success("退出成功");
    }
}