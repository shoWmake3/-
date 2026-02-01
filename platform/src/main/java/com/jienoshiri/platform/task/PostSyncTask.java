package com.jienoshiri.platform.task;

import com.jienoshiri.platform.entity.Post;
import com.jienoshiri.platform.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 定期任务：同步 Redis 中的浏览量到 MySQL
 * (解决热点数据高并发写入数据库的问题)
 */
@Component
public class PostSyncTask {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PostMapper postMapper;

    /**
     * 每隔 5 分钟 (300000 毫秒) 执行一次同步
     * 逻辑：遍历 Redis 中所有的 post:view:* 键，把增量加到数据库里，然后删除 Redis 键
     */
    @Scheduled(fixedRate = 300000)
    public void syncViewCounts() {
        System.out.println(">>> 开始执行浏览量同步任务...");

        // 1. 查找所有缓存的浏览量 Key (模式: post:view:帖子ID)
        // 注意：生产环境海量数据建议用 scan 命令，毕设用 keys 没问题
        Set<String> keys = redisTemplate.keys("post:view:*");

        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            try {
                // 2. 解析帖子ID
                // key 格式例如 "post:view:10" -> split 后取第2部分
                String[] parts = key.split(":");
                if (parts.length < 3) continue;

                Long postId = Long.parseLong(parts[2]);

                // 3. 获取 Redis 里的增量数值
                Object viewCountObj = redisTemplate.opsForValue().get(key);
                if (viewCountObj == null) continue;

                Integer viewCount = (Integer) viewCountObj;
                if (viewCount == 0) continue;

                // 4. 更新数据库
                Post post = postMapper.selectById(postId);
                if (post != null) {
                    // 数据库原有浏览量 + Redis 新增浏览量
                    post.setViewCount(post.getViewCount() + viewCount);
                    postMapper.updateById(post);

                    // 5. 同步完成后，删除 Redis 里的这个 Key (防止重复累加)
                    // 下次有新访问时，会重新生成这个 Key 从 0 开始计数
                    redisTemplate.delete(key);

                    System.out.println(">>> 同步成功: 帖子ID=" + postId + ", 新增浏览量=" + viewCount);
                }
            } catch (Exception e) {
                System.err.println(">>> 同步失败 Key: " + key + ", 原因: " + e.getMessage());
            }
        }
    }
}