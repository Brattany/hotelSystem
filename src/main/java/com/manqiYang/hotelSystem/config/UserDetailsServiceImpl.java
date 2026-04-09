package com.manqiYang.hotelSystem.config;

import com.manqiYang.hotelSystem.entity.guest.Guest;
import com.manqiYang.hotelSystem.entity.user.SysUser;
import com.manqiYang.hotelSystem.mapper.guest.GuestMapper;
import com.manqiYang.hotelSystem.mapper.user.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private GuestMapper guestMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        // 先查guest表
        Guest guest = guestMapper.selectByPhone(phone);
        if (guest != null) {
            return new UserDetailsImpl(guest.getGuestId(), guest.getPhone(), "GUEST");
        }

        // 再查sys_user表
        SysUser sysUser = userMapper.selectByName(phone); // 假设username是phone
        if (sysUser != null) {
            String role = "STAFF"; // 默认STAFF，可根据sysUser.role判断
            if ("admin".equalsIgnoreCase(sysUser.getRole())) {
                role = "ADMIN";
            }
            return new UserDetailsImpl(sysUser.getUserId(), sysUser.getPhone(), role);
        }

        throw new UsernameNotFoundException("User not found: " + phone);
    }
}
