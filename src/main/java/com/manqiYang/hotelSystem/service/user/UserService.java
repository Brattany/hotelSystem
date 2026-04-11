package com.manqiYang.hotelSystem.service.user;

import com.manqiYang.hotelSystem.dto.user.LoginByCodeRequest;
import com.manqiYang.hotelSystem.dto.user.LoginByPasswordRequest;
import com.manqiYang.hotelSystem.dto.user.RegisterRequest;
import com.manqiYang.hotelSystem.entity.user.SysUser;

import java.util.List;

public interface UserService {
    //用户注册
    void register(RegisterRequest request);

    //用户登录
    String login(LoginByPasswordRequest check);

    //手机号+验证码登录
    String loginByPhone(LoginByCodeRequest request);

    //发送验证码
    String sendCode(String phone);

    //修改用户名
    boolean rename(Long userId, String newName);

    //修改密码
    boolean passwordChange(Long userId, String oldPass, String newPass);

    //修改手机号
    boolean phoneChange(Long userId, String newPhone);

    //修改用户状态
    boolean statusChange(Long userId, Integer status);

    //逻辑删除
    boolean delete(Long id);

    //查询某酒店全部用户
    List<SysUser> getByHotelId(Long hotelId);

    //获取用户信息
    SysUser getByPhone(String phone);

    //根据ID查询用户
    SysUser getById(Long id);

    //根据状态查询用户
    List<SysUser> getByStatus(Integer status);

    //根据角色查询用户
    List<SysUser> getByRole(String role);

    //修改职务
    boolean updateRole(Long userId, String role);
}
