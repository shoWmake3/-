package com.jienoshiri.platform.utils;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct; // 注意：Spring Boot 3 用 jakarta
import java.io.InputStream;
import java.util.UUID;

@Component
public class OssUtil {

    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.access-key}")
    private String accessKey;
    @Value("${minio.secret-key}")
    private String secretKey;
    @Value("${minio.bucket-name}")
    private String bucketName;

    private MinioClient minioClient;

    // ⭐ 核心修改：项目启动时自动执行初始化
    @PostConstruct
    public void init() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            // 1. 检查桶是否存在，不存在就创建
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                System.out.println(">>> MinIO 桶 " + bucketName + " 创建成功！");
            }

            // 2. ⭐⭐ 强制设置桶为 Public (公开只读) ⭐⭐
            // 这段 JSON 是标准的 AWS S3 策略，允许任何人(Principal: *)读取(s3:GetObject)
            String policyJson = "{\n" +
                    "    \"Version\": \"2012-10-17\",\n" +
                    "    \"Statement\": [\n" +
                    "        {\n" +
                    "            \"Effect\": \"Allow\",\n" +
                    "            \"Principal\": {\n" +
                    "                \"AWS\": [\n" +
                    "                    \"*\"\n" +
                    "                ]\n" +
                    "            },\n" +
                    "            \"Action\": [\n" +
                    "                \"s3:GetObject\"\n" +
                    "            ],\n" +
                    "            \"Resource\": [\n" +
                    "                \"arn:aws:s3:::" + bucketName + "/*\"\n" +
                    "            ]\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}";

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder().bucket(bucketName).config(policyJson).build()
            );
            System.out.println(">>> MinIO 桶 " + bucketName + " 已设置为公开访问权限！");

        } catch (Exception e) {
            System.err.println(">>> MinIO 初始化失败: " + e.getMessage());
            // 不抛异常，防止影响主程序启动，但这通常意味着 MinIO 没开或者配置不对
        }
    }

    /**
     * 上传文件
     */
    public String uploadFile(MultipartFile file) {
        // init() 会在启动时自动跑，这里不需要重复调，但为了保险判空一下
        if (minioClient == null) init();

        try {
            String originalFilename = file.getOriginalFilename();
            // 防止文件名为空
            String suffix = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String fileName = UUID.randomUUID().toString() + suffix;

            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 返回 URL
            return endpoint + "/" + bucketName + "/" + fileName;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}