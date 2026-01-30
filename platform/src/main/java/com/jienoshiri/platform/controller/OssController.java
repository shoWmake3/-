package com.jienoshiri.platform.controller;

import com.jienoshiri.platform.utils.OssUtil; // 引用刚才写的工具类
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/oss")
public class OssController {

    @Autowired
    private OssUtil ossUtil;

    /**
     * 通用上传接口
     */
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return "文件为空";

        String url = ossUtil.uploadFile(file);

        if (url != null) {
            return url; // 直接返回 OSS 地址，例如 "http://localhost:9000/..."
        } else {
            return "上传失败，请检查 OSS 服务状态";
        }
    }
}