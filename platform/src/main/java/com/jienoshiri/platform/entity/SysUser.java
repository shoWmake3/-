package com.jienoshiri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String nickname;
    private String email;         // 新增
    private String phone;         // 新增
    private String avatar;        // 新增
    private String identityType;
    private Integer reputation;
    private Boolean isVerified;   // 对应 tinyint(1)
    private Integer status;       // 新增: 1正常 0封禁
    private LocalDateTime createTime;
    private LocalDateTime updateTime; // 新增
}