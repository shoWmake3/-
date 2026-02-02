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

    private String role;

    // 在 SysUser 实体类中建议添加一个获取权重的逻辑
    public double calculateWeight() {
        if (this.reputation == null || this.reputation < 0) return 0.0;

        // 根据任务书中的“身份选择”与“信誉模块”设计
        // 基础权重为 1.0。
        // 每 100 点声望增加 0.5 的权重权重上限设为 5.0 (可根据答辩需求调整)
        double weight = 1.0 + (this.reputation / 100.0) * 0.5;
        return Math.min(weight, 5.0);
    }
}