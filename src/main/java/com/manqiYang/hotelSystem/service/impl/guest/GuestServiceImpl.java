package com.manqiYang.hotelSystem.service.impl.guest;

import com.manqiYang.hotelSystem.dto.guest.GuestProfileResponse;
import com.manqiYang.hotelSystem.dto.guest.LoginRequest;
import com.manqiYang.hotelSystem.dto.guest.RegisterRequest;
import com.manqiYang.hotelSystem.entity.guest.Guest;
import com.manqiYang.hotelSystem.mapper.guest.GuestMapper;
import com.manqiYang.hotelSystem.service.guest.GuestService;
import com.manqiYang.hotelSystem.util.jwt.JwtUtil;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONObject;

@Service
public class GuestServiceImpl implements GuestService {

    @Autowired
    public GuestMapper guestMapper;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private static final String CODE_KEY = "guest:code_";

    private final String WX_APPID = "wxd15af1d606eef0a4";
    private final String WX_SECRET = "86c2a6939c62f8fb7bc46474dffa28e2";

    //微信静默登录
    @Override
    public String wxLogin(String wxCode){
        String url = String.format("https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code", WX_APPID, WX_SECRET, wxCode);
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);
        
        JSONObject jsonObject = JSONObject.parseObject(response);
        if (jsonObject.getString("errcode") != null && !jsonObject.getString("errcode").equals("0")) {
            throw new RuntimeException("微信登录失败: " + jsonObject.getString("errmsg"));
        }
        String openId = jsonObject.getString("openid");

        // 2. 根据 openId 查找数据库
        Guest guest = guestMapper.selectByOpenId(openId);

        if (guest == null) {
            // 用户未绑定手机号，返回特定的前缀或抛出特定异常
            // 前端收到 "UNBOUND:" 开头的字符串时，就知道要跳转到绑定手机号页面
            return "UNBOUND:" + openId;
        }

        // 3. 已绑定，直接返回 Token
        return JwtUtil.createToken(guest.getGuestId(), guest.getPhone(), guest.getOpenId());
    }

    @Override
    public boolean register(RegisterRequest registerRequest) {

        String name = registerRequest.getGuestName();
        String phone = registerRequest.getPhone();
        String openId = registerRequest.getOpenId();
        String code = redisTemplate.opsForValue().get(CODE_KEY + phone);
        
        Guest guest = new Guest();
        guest.setName(name);
        guest.setPhone(phone);
        guest.setOpenId(openId);

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

        if (loginRequest.getOpenId() != null && !loginRequest.getOpenId().isEmpty()) {
            if (guest.getOpenId() == null || guest.getOpenId().isEmpty()) {
                guest.setOpenId(loginRequest.getOpenId());
                guestMapper.updateById(guest); // 更新数据库，完成绑定
            }
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
    public GuestProfileResponse update(Guest guest){
        if (guest == null || guest.getGuestId() == null) {
            throw new RuntimeException("缺少用户编号，无法更新资料");
        }

        Guest existing = guestMapper.selectById(guest.getGuestId());
        if (existing == null) {
            throw new RuntimeException("当前用户不存在");
        }

        String nextName = guest.getName() != null ? guest.getName().trim() : "";
        String nextPhone = guest.getPhone() != null ? guest.getPhone().trim() : "";
        String nextIdCard = guest.getIdCard() != null ? guest.getIdCard().trim() : "";

        if (nextName.isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }

        if (nextPhone.isEmpty() || !nextPhone.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("请输入正确的手机号");
        }

        Guest samePhoneGuest = guestMapper.selectByPhone(nextPhone);
        if (samePhoneGuest != null && !samePhoneGuest.getGuestId().equals(existing.getGuestId())) {
            throw new RuntimeException("该手机号已被其他账号使用");
        }

        existing.setName(nextName);
        existing.setPhone(nextPhone);
        existing.setIdCard(nextIdCard.isEmpty() ? null : nextIdCard);

        if (guest.getOpenId() != null && !guest.getOpenId().trim().isEmpty()) {
            existing.setOpenId(guest.getOpenId().trim());
        }

        boolean updated = guestMapper.updateById(existing);
        if (!updated) {
            throw new RuntimeException("个人资料保存失败");
        }

        return buildGuestProfileResponse(existing);
    }

    @Override
    public boolean delete(Long guestId){
        return guestMapper.deleteById(guestId);
    }

    private GuestProfileResponse buildGuestProfileResponse(Guest guest) {
        GuestProfileResponse response = new GuestProfileResponse();
        response.setGuestId(guest.getGuestId());
        response.setName(guest.getName());
        response.setPhone(guest.getPhone());
        response.setIdCard(guest.getIdCard());
        response.setToken(JwtUtil.createToken(guest.getGuestId(), guest.getPhone(), guest.getOpenId()));
        return response;
    }
}
