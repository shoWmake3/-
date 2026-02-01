package com.jienoshiri.platform.controller;

import com.jienoshiri.platform.entity.Wiki;
import com.jienoshiri.platform.service.WikiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wiki")
public class WikiController {

    @Autowired
    private WikiService wikiService;

    // 获取列表
    @GetMapping("/list")
    public List<Wiki> getList() {
        return wikiService.getAll();
    }

    // 搜索
    @GetMapping("/search")
    public List<Wiki> search(@RequestParam String keyword) {
        return wikiService.search(keyword);
    }

    // 详情
    @GetMapping("/detail/{id}")
    public Wiki getDetail(@PathVariable Long id) {
        return wikiService.getById(id);
    }

    // ⭐ 帖子转 Wiki (管理员或高声望用户调用)
    @PostMapping("/convert")
    public String convertFromPost(@RequestParam Long postId, @RequestParam String category) {
        wikiService.createFromPost(postId, category);
        return "转化成功";
    }
}