package com.jienoshiri.platform;

import org.mybatis.spring.annotation.MapperScan; // 导入这个包
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.jienoshiri.platform.mapper") // ✅ 加上这行，强制扫描 mapper 包
public class PlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }

}