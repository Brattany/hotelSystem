package com.manqiYang.hotelSystem.service.impl.guest;

import com.manqiYang.hotelSystem.dto.guest.LoginRequest;
import com.manqiYang.hotelSystem.dto.guest.RegisterRequest;
import com.manqiYang.hotelSystem.entity.guest.Guest;
import com.manqiYang.hotelSystem.mapper.guest.GuestMapper;
import com.manqiYang.hotelSystem.service.guest.GuestService;
import com.manqiYang.hotelSystem.util.jwt.JwtUtil;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.management.RuntimeErrorException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GuestServiceImpl implements GuestService {

    @Autowired
    public GuestMapper guestMapper;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private static final String CODE_KEY = "guest:code_";

    @Override
    public boolean register(RegisterRequest registerRequest) {

        String name = registerRequest.getGuestName();
        String phone = registerRequest.getPhone();
        String code = redisTemplate.opsForValue().get(CODE_KEY + phone);
        
        Guest guest = new Guest();
        guest.setName(name);
        guest.setPhone(phone);

        //参数校验
        if (name == null || name.length() < 2) {
            throw new RuntimeException("用户名不合法");
        }

        if(code == null || !code.equals(registerRequest.getCode())){
            throw new RuntimeException("验证码错误");
        }

        //判断手机号是否已注册
        Guest exist = guestMapper.selectByPhone(phone);
        if(exist != null){
            throw new RuntimeException("该手机号已注册，请直接登录。");
        }

        //插入数据库
        return guestMapper.insert(guest);
    }

    @Override
    public String login(LoginRequest loginRequest) {
        String code = redisTemplate.opsForValue().get(CODE_KEY + loginRequest.getPhone());
        if(code == null || !code.equals(loginRequest.getCode())){
            throw new RuntimeException("验证码错误");
        }

        Guest guest = guestMapper.selectByPhone(loginRequest.getPhone());

        if(guest == null){
             throw new RuntimeException("用户不存在");
        }

        // 返回token
        return JwtUtil.createToken(guest.getGuestId(),guest.getPhone(),guest.getOpenId());
    }

    //发送验证码
    @Override
    public String sendCode(String phone){
        String code = String.valueOf(new Random().nextInt(900000) + 100000);

        // 存Redis（5分钟过期）
        redisTemplate.opsForValue().set(
                CODE_KEY + phone,
                code,
                5,
                TimeUnit.MINUTES
        );

        // TODO：接入短信服务
        System.out.println("验证码：" + code);
        return code;
    }

    @Override
    public boolean create(Guest guest){
        guest.setIsDelete(0);
        return guestMapper.insert(guest);
    }

    @Override
    public Guest getById(Long guestId){
        return guestMapper.selectById(guestId);
    }

    @Override
    public Guest getByPhone(String phone){
        return guestMapper.selectByPhone(phone);
    }

    @Override
    public Guest getByOpenId(String openId){
        return guestMapper.selectByOpenId(openId);
    }

    @Override
    public boolean update(Guest guest){
        return guestMapper.updateById(guest);
    }

    @Override
    public boolean delete(Long guestId){
        return guestMapper.deleteById(guestId);
    }
}
