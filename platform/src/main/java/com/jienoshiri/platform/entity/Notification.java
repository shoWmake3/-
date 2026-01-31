package com.jienoshiri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;        // 接收人ID
    private String title;       // 标题
    private String content;     // 内容
    private Integer type;       // 0:系统通知 1:点赞 2:评论
    private Long relationId;    // 关联ID (帖子ID或评论ID)
    private Boolean isRead;     // 是否已读
    private LocalDateTime createTime;
}