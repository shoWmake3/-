package com.jienoshiri.platform.controller;

import com.jienoshiri.platform.entity.SysUser;
import com.jienoshiri.platform.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TestController {

    @Autowired
    private UserMapper userMapper;

    // 浏览器访问 http://localhost:8080/test 就能看到结果
    @GetMapping("/test")
    public List<SysUser> test() {
        // selectList(null) 表示查询所有，没有条件
        return userMapper.selectList(null);
    }
}