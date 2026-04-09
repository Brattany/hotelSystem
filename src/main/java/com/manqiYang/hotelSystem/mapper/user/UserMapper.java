package com.manqiYang.hotelSystem.mapper.user;

import com.manqiYang.hotelSystem.entity.user.SysUser;
import com.manqiYang.hotelSystem.mapper.base.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<SysUser, Long> {

    List<SysUser> selectByHotelId(@Param("hotelId") Long hotelId);

    List<SysUser> selectByStatus(@Param("status") Integer status);

    List<SysUser> selectByRole(@Param("role") String role);

    SysUser selectByName(@Param("username") String username);

    SysUser selectByPhone(@Param("phone") String phone);

    boolean updateName(@Param("userId") Long userId, @Param("username") String username);

    boolean updatePassword(@Param("userId") Long userId, @Param("password") String password);

    boolean updateRole(@Param("userId") Long userId, @Param("role") String role);

    boolean updatePhone(@Param("userId") Long userId, @Param("phone") String phone);

    boolean updateStatus(@Param("userId") Long userId, @Param("status") Integer status);
}
