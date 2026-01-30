package com.jienoshiri.platform.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jienoshiri.platform.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {}