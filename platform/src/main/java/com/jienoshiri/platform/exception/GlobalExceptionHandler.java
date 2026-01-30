package com.jienoshiri.platform.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(MaxUploadSizeExceededException e) {
        System.err.println(">>> 触发文件超限异常: " + e.getMessage());
        return "文件太大了！单个文件不能超过200MB";
    }

    @ExceptionHandler(Exception.class)
    public String handleOtherException(Exception e) {
        e.printStackTrace();
        return "服务器内部错误: " + e.getMessage();
    }
}