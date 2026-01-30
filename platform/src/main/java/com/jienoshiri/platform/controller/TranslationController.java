package com.jienoshiri.platform.controller;

import com.jienoshiri.platform.service.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tool")
public class TranslationController {

    @Autowired
    private TranslationService translationService;

    // 翻译接口
    // GET /tool/translate?content=你好&target=ja
    @GetMapping("/translate")
    public String translate(@RequestParam String content, @RequestParam String target) {
        return translationService.translate(content, target);
    }
}