package com.manqiYang.hotelSystem.entity.user;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Data
public class SysUser {

    private Long userId;

    private Long hotelId;

    private String username;

    private String password;

    private String role;

    private String phone;

    /**
     * 状态：1-启用 0-停用
     */
    private Integer status;

    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}