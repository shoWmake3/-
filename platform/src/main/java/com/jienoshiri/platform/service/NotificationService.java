package com.jienoshiri.platform.service;

import com.jienoshiri.platform.entity.Notification;
import com.jienoshiri.platform.mapper.NotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    /**
     * 发送通知的通用方法
     */
    public void send(Long userId, String title, String content, Integer type, Long relationId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        n.setRelationId(relationId);
        n.setIsRead(false);
        n.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(n);
    }
}