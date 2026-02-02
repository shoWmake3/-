package com.jienoshiri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_wiki")
public class Wiki {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;       // 词条标题
    private String summary;     // 摘要
    private String content;     // 详细内容 (Markdown/HTML)
    private String category;    // 分类 (如: 签证, 租房, 交通)
    private String coverImage;  // 封面图
    private Long creatorId;     // 创建人ID (可以是转化的帖子作者)
    private Long sourcePostId;  // 来源帖子ID (如果是从帖子转化的)
    private Integer viewCount;  // 浏览量
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer status; // 0:待审 1:正常 2:拒绝
    private String rejectReason;
}