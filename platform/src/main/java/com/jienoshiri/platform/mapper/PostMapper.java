package com.jienoshiri.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jienoshiri.platform.entity.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
    // MyBatis-Plus 已经帮我们写好了增删改查
}