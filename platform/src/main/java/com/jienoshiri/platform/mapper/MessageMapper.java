package com.jienoshiri.platform.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jienoshiri.platform.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {}