package com.manqiYang.hotelSystem.service.impl.user;

import com.manqiYang.hotelSystem.config.UserDetailsImpl;
import com.manqiYang.hotelSystem.dto.user.LoginByCodeRequest;
import com.manqiYang.hotelSystem.dto.user.LoginByPasswordRequest;
import com.manqiYang.hotelSystem.dto.user.RegisterRequest;
import com.manqiYang.hotelSystem.entity.user.SysUser;
import com.manqiYang.hotelSystem.mapper.user.UserMapper;
import com.manqiYang.hotelSystem.service.user.UserService;
import com.manqiYang.hotelSystem.util.jwt.JwtUtil;
import com.manqiYang.hotelSystem.util.security.PasswordUtil;
import com.manqiYang.hotelSystem.util.validate.UserValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private static final String CODE_KEY = "user:code_";

    @Override
    public SysUser getById(Long id) {
        return userMapper.selectById(id);
    }

    /*********************************/
    /***********新用户注册**************/
    /*********************************/
    @Override
    public void register(RegisterRequest request){
        Long hotelId = request.getHotelId();
        if(hotelId == null){
            throw new RuntimeException("系统未能识别当前酒店信息，请重新进入酒店页面");
        }

        String userName = request.getUserName();
        String password = request.getPassword();
        String role = request.getRole();
        String phone = request.getPhone();
        String code = redisTemplate.opsForValue().get(CODE_KEY + phone);

        SysUser user = new SysUser();
        user.setHotelId(hotelId);
        user.setUsername(userName);
        user.setRole(role);
        user.setPhone(phone);

        //参数校验
        if (userName == null || userName.length() < 2) {
            throw new RuntimeException("用户名不合法");
        }

        if (password == null || password.length() < 6) {
            throw new RuntimeException("密码长度至少6位");
        }

        if(code == null || !code.equals(request.getCode())){
            throw new RuntimeException("验证码错误");
        }

        //判断用户是否存在
        SysUser exist = userMapper.selectByName(userName);
        if (exist != null) {
            throw new RuntimeException("用户名已存在");
        }
        if(userMapper.selectByPhone(phone) != null){
            throw new RuntimeException("手机号已注册");
        }

        //密码加密
        String encodePwd = PasswordUtil.encode(password);
        user.setPassword(encodePwd);

        //默认状态
        user.setStatus(1); // 1-启用
        user.setIsDelete(0);

        userMapper.insert(user);
    }

    /*********************************/
    /************用户登录**************/
    /*********************************/
    @Override
    public String login(LoginByPasswordRequest check){
        SysUser user = userMapper.selectByPhone(check.getPhone());

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 密码校验（加密）
        if (!PasswordUtil.matches(check.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        if (user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }

        // 返回用户token
        return JwtUtil.createToken(user.getUserId(), user.getPhone(), user.getRole());
    }

    /*********************************/
    /************手机号+验证码登录**************/
    /*********************************/
    @Override
    public String loginByPhone(LoginByCodeRequest request){
        String code = redisTemplate.opsForValue().get(CODE_KEY + request.getPhone());

        if(code == null || !code.equals(request.getCode())){
            throw new RuntimeException("验证码错误");
        }

        SysUser user = userMapper.selectByPhone(request.getPhone());

        if(user == null){
            throw new RuntimeException("用户不存在");
        }

        // 返回用户token
        return JwtUtil.createToken(user.getUserId(), user.getPhone(), user.getRole());
    }

    /*********************************/
    /************发送验证码**************/
    /*********************************/
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

    /*********************************/
    /***********获取用户信息*************/
    /*********************************/
    @Override
    public SysUser getByPhone(String phone){
        return userMapper.selectByPhone(phone);
    }

    /*********************************/
    /***********修改用户名**************/
    /*********************************/
    @Override
    public boolean rename(Long userId, String newName){

        if (newName == null || newName.length() < 2) {
            throw new RuntimeException("用户名不合法");
        }

        SysUser exist = userMapper.selectByName(newName);
        if (exist != null) {
            throw new RuntimeException("用户名已存在");
        }

        return userMapper.updateName(userId, newName);
    }

    /*********************************/
    /************修改密码**************/
    /*********************************/
    @Override
    public boolean passwordChange(Long userId, String oldPass, String newPass){

        SysUser user = userMapper.selectById(userId);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 校验旧密码
        if (!PasswordUtil.matches(oldPass, user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }

        if (PasswordUtil.matches(newPass, user.getPassword())) {
            throw new RuntimeException("新旧密码不能相同");
        }

        String newEncode = PasswordUtil.encode(newPass);

        return userMapper.updatePassword(userId, newEncode);
    }

    /*********************************/
    /************更换电话号码************/
    /*********************************/
    @Override
    public boolean phoneChange(Long userId, String newPhone){

        UserValidator.validatePhone(newPhone);

        return userMapper.updatePhone(userId, newPhone);
    }

    /*********************************/
    /************修改用户状态************/
    /*********************************/
    @Override
    public boolean statusChange(Long userId, Integer status){

        return userMapper.updateStatus(userId, status);
    }

    /*********************************/
    /************用户注销**************/
    /*********************************/
    @Override
    public boolean delete(Long id){
        return userMapper.deleteById(id);
    }

    @Override
    public List<SysUser> getByHotelId(Long hotelId){
        return userMapper.selectByHotelId(hotelId);
    }

    @Override
    public List<SysUser> getByStatus(Integer status){
        return userMapper.selectByStatus(status);
    }

    @Override
    public List<SysUser> getByRole(String role){
        return userMapper.selectByRole(role);
    }

    @Override
    public boolean updateRole(Long userId, String role){
        return userMapper.updateRole(userId, role);
    }
}
