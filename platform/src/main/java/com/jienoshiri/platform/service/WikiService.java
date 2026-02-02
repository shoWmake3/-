package com.jienoshiri.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jienoshiri.platform.document.WikiDoc;
import com.jienoshiri.platform.entity.Post;
import com.jienoshiri.platform.entity.Wiki;
import com.jienoshiri.platform.mapper.PostMapper;
import com.jienoshiri.platform.mapper.WikiMapper;
import com.jienoshiri.platform.repository.WikiEsRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WikiService {

    @Autowired
    private WikiMapper wikiMapper;

    @Autowired
    private WikiEsRepository wikiEsRepository;

    @Autowired
    private PostMapper postMapper;

    /**
     * 1. 创建/更新 Wiki (同时同步到 MySQL 和 ES)
     */
    @Transactional
    public void saveWiki(Wiki wiki) {
        boolean isNew = (wiki.getId() == null);
        if (isNew) {
            wiki.setCreateTime(LocalDateTime.now());
            wiki.setViewCount(0);
            // 注意：如果是直接调用 saveWiki，状态由调用方决定
            // 如果没设状态，默认给个 0 (待审) 安全点，或者根据业务需求定
            if (wiki.getStatus() == null) {
                wiki.setStatus(0);
            }
            wikiMapper.insert(wiki);
        } else {
            wiki.setUpdateTime(LocalDateTime.now());
            wikiMapper.updateById(wiki);
        }

        // 同步到 Elasticsearch
        // 只有当状态为 1 (正常) 时，才应该被普通用户搜到
        // 这里简单处理：直接同步进去，搜索时 ES 那边最好也过滤 status，
        // 或者我们依赖 searchFromDb 的降级逻辑来过滤。
        // 为了演示方便，我们先把数据同步过去。
        try {
            WikiDoc doc = new WikiDoc();
            BeanUtils.copyProperties(wiki, doc);
            wikiEsRepository.save(doc);
        } catch (Exception e) {
            System.err.println("ES 同步失败 (可能没启动): " + e.getMessage());
        }
    }

    /**
     * 2. 全文搜索 (优先查 ES，失败查 DB)
     * ⭐ 修改：增加状态过滤，只展示已通过(status=1)的
     */
    public List<Wiki> search(String keyword) {
        try {
            // ES 搜索逻辑 (如果有 ES 并且数据量大，这里需要写 NativeSearchQueryBuilder 来过滤 status=1)
            // 鉴于目前 ES 配置较简单，为了保证审核逻辑生效，我们优先演示 DB 查询 (稳健性高)
            // 或者：您可以在 WikiDoc 里加 status 字段，然后 findByTitleOrContentAndStatus(..., ..., 1)

            // 暂时降级直接查 DB，确保逻辑正确
            return searchFromDb(keyword);
        } catch (Exception e) {
            return searchFromDb(keyword);
        }
    }

    /**
     * 数据库兜底搜索
     */
    private List<Wiki> searchFromDb(String keyword) {
        QueryWrapper<Wiki> query = new QueryWrapper<>();
        // ⭐ 关键逻辑：(status = 1) AND (title like %k% OR content like %k%)
        query.eq("status", 1)
                .and(wrapper -> wrapper.like("title", keyword).or().like("content", keyword));

        return wikiMapper.selectList(query);
    }

    /**
     * 3. ⭐ 核心功能：将帖子转化为 Wiki
     */
    @Transactional
    public void createFromPost(Long postId, String category) {
        Post post = postMapper.selectById(postId);
        if (post == null) throw new RuntimeException("帖子不存在");

        Wiki wiki = new Wiki();
        wiki.setTitle(post.getTitle());
        wiki.setContent(post.getContent());

        // 简单摘要：取前50字
        String plainText = post.getContent().replaceAll("<[^>]+>", ""); // 去除HTML标签(如果有)
        wiki.setSummary(plainText.length() > 50 ? plainText.substring(0, 50) + "..." : plainText);

        wiki.setCategory(category);
        wiki.setCreatorId(post.getUserId());
        wiki.setSourcePostId(post.getId());

        // 封面图处理 (取帖子第一张图)
        if (post.getMediaUrls() != null && post.getMediaUrls().contains("http")) {
            // 简单粗暴取第一个http链接，实际建议用 JSON 解析
            // wiki.setCoverImage(...);
        }

        // ⭐ 关键修改：默认状态为 0 (待审核)，需要管理员后台审核通过后才能看到
        wiki.setStatus(0);

        saveWiki(wiki);

        // 可选：将帖子状态改为“已收录”(status=3)，防止重复收录
        post.setStatus(3);
        postMapper.updateById(post);
    }

    /**
     * 获取所有通过审核的词条
     */
    public List<Wiki> getAll() {
        QueryWrapper<Wiki> query = new QueryWrapper<>();
        query.eq("status", 1); // ⭐ 只查已通过的
        query.orderByDesc("view_count"); // 按热度排
        return wikiMapper.selectList(query);
    }

    public Wiki getById(Long id) {
        return wikiMapper.selectById(id);
    }
}