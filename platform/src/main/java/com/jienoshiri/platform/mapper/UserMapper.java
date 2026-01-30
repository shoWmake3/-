package com.jienoshiri.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jienoshiri.platform.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<SysUser> {
    // 没错，里面什么都不用写！
    // MyBatis-Plus 自动帮你实现了 增删改查 方法
}