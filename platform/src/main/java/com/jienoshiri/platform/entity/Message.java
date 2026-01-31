package com.jienoshiri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long senderId;      // 发送者
    private Long receiverId;    // 接收者
    private String content;     // 内容
    private Boolean isRead;     // 是否已读
    private LocalDateTime createTime;
}