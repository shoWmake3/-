package com.jienoshiri.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jienoshiri.platform.entity.PostLike;
import com.jienoshiri.platform.mapper.PostLikeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private PostLikeMapper postLikeMapper;

    @Autowired
    private com.jienoshiri.platform.mapper.PostMapper postMapper; // 注入 PostMapper

    /**
     * 核心算法：获取 UserCF 推荐的帖子ID列表
     * @param targetUserId 当前用户ID
     * @param maxNum 推荐数量(比如10条)
     * @return 帖子ID列表
     */
    public List<Long> recommendPostIds(Long targetUserId, int maxNum) {
        // 1. 获取全站所有的点赞数据
        // (注意：实际生产环境数据量大时不能这么查，要用离线计算。但毕设数据量小，全查内存计算是最快的MVP方案)
        List<PostLike> allLikes = postLikeMapper.selectList(null);

        // 2. 数据转换：Map<用户ID, Set<他点赞过的帖子ID>>
        Map<Long, Set<Long>> userLikeMap = new HashMap<>();
        for (PostLike like : allLikes) {
            userLikeMap.computeIfAbsent(like.getUserId(), k -> new HashSet<>()).add(like.getPostId());
        }

        // 如果当前用户没点过赞，或者是新用户，直接返回空(降级策略)
        if (!userLikeMap.containsKey(targetUserId)) {
            return Collections.emptyList();
        }

        Set<Long> targetUserLikes = userLikeMap.get(targetUserId);
        Map<Long, Double> userSimilarityMap = new HashMap<>();

        // 3. 计算用户相似度 (Jaccard Similarity)
        // 相似度 = (交集) / (并集)
        for (Long otherUserId : userLikeMap.keySet()) {
            if (otherUserId.equals(targetUserId)) continue; // 跳过自己

            Set<Long> otherUserLikes = userLikeMap.get(otherUserId);

            // 计算交集 (两人都赞过的)
            Set<Long> intersection = new HashSet<>(targetUserLikes);
            intersection.retainAll(otherUserLikes);

            if (intersection.isEmpty()) continue; // 没有共同语言，跳过

            // 计算并集
            Set<Long> union = new HashSet<>(targetUserLikes);
            union.addAll(otherUserLikes);

            // 相似度公式
            double similarity = (double) intersection.size() / union.size();
            userSimilarityMap.put(otherUserId, similarity);
        }

        // 4. 找到最相似的 Top 5 用户
        List<Long> similarUsers = userSimilarityMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed()) // 按相似度降序
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 5. 聚合推荐结果
        // 逻辑：相似用户喜欢、但我还没看过的帖子
        Map<Long, Double> recommendPostScore = new HashMap<>();

        for (Long similarUser : similarUsers) {
            double simScore = userSimilarityMap.get(similarUser); // 相似度作为权重
            Set<Long> hisLikes = userLikeMap.get(similarUser);

            for (Long postId : hisLikes) {
                // 过滤掉我已经点过赞的
                if (targetUserLikes.contains(postId)) continue;

                // 累加推荐分
                recommendPostScore.merge(postId, simScore, Double::sum);
            }
        }

        // 6. 按分数排序，取前 N 个
        return recommendPostScore.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxNum)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 基于内容的推荐 (Content-Based)
     * 逻辑：根据用户身份 (student/worker) 推荐对应板块 (study/work)
     * @param identityType 用户身份
     * @return 推荐的帖子ID列表
     */
    public List<Long> recommendByContent(String identityType) {
        if (identityType == null) return Collections.emptyList();

        QueryWrapper<com.jienoshiri.platform.entity.Post> query = new QueryWrapper<>();

        // 简单粗暴的规则匹配
        if ("student".equals(identityType)) {
            // 留学生 -> 推荐 "study"(学习) 和 "life"(生活)
            query.in("category", "study", "life");
        } else if ("worker".equals(identityType)) {
            // 打工人 -> 推荐 "work"(工作) 和 "life"(生活)
            query.in("category", "work", "life");
        } else if ("tourist".equals(identityType)) {
            // 游客 -> 推荐 "life"(生活)
            query.eq("category", "life");
        } else {
            return Collections.emptyList();
        }

        // 取最新的 10 条
        query.orderByDesc("create_time");
        query.last("LIMIT 10");

        return postMapper.selectList(query).stream()
                .map(com.jienoshiri.platform.entity.Post::getId)
                .collect(Collectors.toList());
    }
}