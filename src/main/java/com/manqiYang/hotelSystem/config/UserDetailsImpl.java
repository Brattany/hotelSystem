package com.manqiYang.hotelSystem.config;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Data
public class UserDetailsImpl implements UserDetails {

    private Long id;
    private String phone;
    private String role;

    public UserDetailsImpl() {}

    public UserDetailsImpl(Long id, String phone, String role) {
        this.id = id;
        this.phone = phone;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
