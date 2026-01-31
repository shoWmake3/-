package com.jienoshiri.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jienoshiri.platform.entity.Message;
import com.jienoshiri.platform.entity.Notification;
import com.jienoshiri.platform.entity.SysUser;
import com.jienoshiri.platform.mapper.MessageMapper;
import com.jienoshiri.platform.mapper.NotificationMapper;
import com.jienoshiri.platform.mapper.UserMapper;
import com.jienoshiri.platform.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private NotificationMapper notificationMapper;
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;

    // --- 通知模块 ---

    // 获取我的通知列表
    @GetMapping("/notifications")
    public List<Notification> getMyNotifications(@RequestHeader("Authorization") String token) {
        Long userId = getUserIdByToken(token);
        QueryWrapper<Notification> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        query.orderByDesc("create_time");
        return notificationMapper.selectList(query);
    }

    // 标记通知为已读
    @PostMapping("/notification/read/{id}")
    public String readNotification(@PathVariable Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n != null) {
            n.setIsRead(true);
            notificationMapper.updateById(n);
        }
        return "success";
    }

    // --- 私信模块 ---

    // 获取我和某人的聊天记录
    @GetMapping("/chat")
    public List<Message> getChatHistory(@RequestParam Long targetUserId, @RequestHeader("Authorization") String token) {
        Long myId = getUserIdByToken(token);
        QueryWrapper<Message> query = new QueryWrapper<>();
        // (sender = me AND receiver = target) OR (sender = target AND receiver = me)
        query.and(wrapper -> wrapper
                .eq("sender_id", myId).eq("receiver_id", targetUserId)
                .or()
                .eq("sender_id", targetUserId).eq("receiver_id", myId)
        );
        query.orderByAsc("create_time");
        return messageMapper.selectList(query);
    }

    // 发送私信
    @PostMapping("/send")
    public String sendMessage(@RequestBody Message msg, @RequestHeader("Authorization") String token) {
        Long myId = getUserIdByToken(token);
        msg.setSenderId(myId);
        msg.setIsRead(false);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
        return "success";
    }

    // 辅助方法：从 Token 获取用户ID
    private Long getUserIdByToken(String token) {
        String username = jwtUtil.getUsername(token);
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));
        return user.getId();
    }

    /**
     * ⭐ 新增：获取我的私信会话列表 (最近联系人)
     * 逻辑：查出所有相关消息 -> 按对方ID分组 -> 取最新一条 -> 拼装用户信息
     */
    @GetMapping("/conversations")
    public List<ConversationVo> getConversations(@RequestHeader("Authorization") String token) {
        Long myId = getUserIdByToken(token);

        // 1. 查出所有与我有关的消息，按时间倒序
        QueryWrapper<Message> query = new QueryWrapper<>();
        query.and(w -> w.eq("sender_id", myId).or().eq("receiver_id", myId));
        query.orderByDesc("create_time");
        List<Message> allMessages = messageMapper.selectList(query);

        // 2. 内存分组，取每个对话的最新一条
        Map<Long, Message> latestMsgMap = new LinkedHashMap<>();
        for (Message msg : allMessages) {
            Long otherId = msg.getSenderId().equals(myId) ? msg.getReceiverId() : msg.getSenderId();
            if (!latestMsgMap.containsKey(otherId)) {
                latestMsgMap.put(otherId, msg); // 只放第一条（因为是倒序，所以是最新）
            }
        }

        // 3. 拼装 VO 数据
        List<ConversationVo> result = new ArrayList<>();
        for (Map.Entry<Long, Message> entry : latestMsgMap.entrySet()) {
            Long otherId = entry.getKey();
            Message msg = entry.getValue();

            SysUser otherUser = userMapper.selectById(otherId);
            if (otherUser != null) {
                ConversationVo vo = new ConversationVo();
                vo.setTargetUserId(otherUser.getId());
                vo.setTargetName(otherUser.getNickname());
                vo.setTargetAvatar(otherUser.getAvatar());
                vo.setLatestContent(msg.getContent());
                vo.setLatestTime(msg.getCreateTime());
                result.add(vo);
            }
        }
        return result;
    }
    // 内部 DTO 类 (可以直接放在 Controller 文件最下面)
    @lombok.Data
    public static class ConversationVo {
        private Long targetUserId;
        private String targetName;
        private String targetAvatar;
        private String latestContent;
        private LocalDateTime latestTime;
    }
}