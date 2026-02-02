package com.jienoshiri.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("post")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;          // 作者ID
    private String title;         // 标题
    private String content;       // 内容
    private String mediaUrls;     // 图片/视频 (JSON字符串)
    private String category;      // 分类
    private String language;      // 语种: zh/ja

    // LBS 信息
    private String locationName;
    private BigDecimal latitude;
    private BigDecimal longitude;

    // 统计数据
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;

    // 状态: 0待审核, 1正常, 2驳回, 3已转Wiki
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ⭐⭐ 新增：拒绝理由 (之前漏加了这个) ⭐⭐
    private String rejectReason;
}