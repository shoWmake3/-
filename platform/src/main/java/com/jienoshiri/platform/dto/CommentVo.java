package com.jienoshiri.platform.dto;

import com.jienoshiri.platform.entity.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CommentVo extends Comment {
    // 评论人的信息
    private String nickname;
    private String avatar;
    private String identityType;
    private Integer reputation;
}