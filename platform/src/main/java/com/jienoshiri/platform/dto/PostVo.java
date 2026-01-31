package com.jienoshiri.platform.dto;

import com.jienoshiri.platform.entity.Post;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true) // 继承父类的属性
public class PostVo extends Post {
    // 在原有的 Post 属性基础上，额外增加作者信息
    private String authorName;
    private String authorAvatar;
    private String authorIdentity; // 比如：留学生、中介
    private Integer authorReputation;
    private Double distance; // 新增：距离当前用户的千米数
    private Boolean isLiked; // 当前用户是否点过赞
    private Double score;    // 推荐算法算出的分数 (仅内部排序用)
}