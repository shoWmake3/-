package com.jienoshiri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;
    private Long userId;
    private String content;
    private LocalDateTime createTime;

    // ⭐ 新增字段：评分 (1.0 - 5.0)，如果是普通评论则为 null 或 0
    private Double score;

    // ⭐ 新增字段：权重快照 (记录评价时该用户的权重)
    // 为什么要有快照？因为用户以后的声望会变，但我们要保留他当时评价时的“分量”。
    private Double weightSnapshot;
}