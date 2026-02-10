package com.jienoshiri.platform.controller;

import com.jienoshiri.platform.entity.Notification;
import com.jienoshiri.platform.entity.SysUser;
import com.jienoshiri.platform.mapper.MessageMapper;
import com.jienoshiri.platform.mapper.NotificationMapper;
import com.jienoshiri.platform.mapper.UserMapper;
import com.jienoshiri.platform.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MessageController controller = new MessageController();
        ReflectionTestUtils.setField(controller, "notificationMapper", notificationMapper);
        ReflectionTestUtils.setField(controller, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(controller, "userMapper", userMapper);
        ReflectionTestUtils.setField(controller, "jwtUtil", jwtUtil);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void readNotification_whenOwnerMatches_shouldMarkAsRead() throws Exception {
        String token = "Bearer token-a";
        Long currentUserId = 1L;
        Long notificationId = 100L;

        SysUser userA = new SysUser();
        userA.setId(currentUserId);
        userA.setUsername("userA");

        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(currentUserId);
        notification.setIsRead(false);

        when(jwtUtil.getUsername(token)).thenReturn("userA");
        when(userMapper.selectOne(any())).thenReturn(userA);
        when(notificationMapper.selectById(notificationId)).thenReturn(notification);

        mockMvc.perform(post("/message/notification/read/{id}", notificationId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        verify(notificationMapper).updateById(notification);
    }

    @Test
    void readNotification_whenOwnerMismatches_shouldReturnForbidden() throws Exception {
        String token = "Bearer token-a";
        Long currentUserId = 1L;
        Long notificationId = 200L;

        SysUser userA = new SysUser();
        userA.setId(currentUserId);
        userA.setUsername("userA");

        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(2L);
        notification.setIsRead(false);

        when(jwtUtil.getUsername(token)).thenReturn("userA");
        when(userMapper.selectOne(any())).thenReturn(userA);
        when(notificationMapper.selectById(notificationId)).thenReturn(notification);

        mockMvc.perform(post("/message/notification/read/{id}", notificationId)
                        .header("Authorization", token))
                .andExpect(status().isForbidden());

        verify(notificationMapper, never()).updateById(any(Notification.class));
    }
}
