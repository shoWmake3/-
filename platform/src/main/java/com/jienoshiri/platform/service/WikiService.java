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
import java.util.stream.Collectors;

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
            wikiMapper.insert(wiki);
        } else {
            wiki.setUpdateTime(LocalDateTime.now());
            wikiMapper.updateById(wiki);
        }

        // 同步到 Elasticsearch
        WikiDoc doc = new WikiDoc();
        BeanUtils.copyProperties(wiki, doc);
        wikiEsRepository.save(doc);
    }

    /**
     * 2. 全文搜索 (优先查 ES)
     */
    public List<Wiki> search(String keyword) {
        // 这里简单演示：如果在 ES 查不到或没配好，降级查 DB
        try {
            // ES 搜索逻辑 (略简化，实际可用 NativeSearchQuery)
            // List<WikiDoc> docs = wikiEsRepository.findByTitleOrContent(keyword, keyword);
            // 这里为了演示稳健性，先直接查 DB LIKE，您可以稍后开启 ES
            return searchFromDb(keyword);
        } catch (Exception e) {
            return searchFromDb(keyword);
        }
    }

    private List<Wiki> searchFromDb(String keyword) {
        QueryWrapper<Wiki> query = new QueryWrapper<>();
        query.like("title", keyword).or().like("content", keyword);
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
        wiki.setSummary(post.getContent().length() > 50 ? post.getContent().substring(0, 50) + "..." : post.getContent());
        wiki.setCategory(category);
        wiki.setCreatorId(post.getUserId());
        wiki.setSourcePostId(post.getId());

        // 封面图处理 (取帖子第一张图)
        // logic...

        saveWiki(wiki);
    }

    public List<Wiki> getAll() {
        return wikiMapper.selectList(null);
    }

    public Wiki getById(Long id) {
        return wikiMapper.selectById(id);
    }
}