package com.jienoshiri.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jienoshiri.platform.dto.PostVo;
import com.jienoshiri.platform.entity.Post;
import com.jienoshiri.platform.entity.SysUser;
import com.jienoshiri.platform.mapper.CommentMapper;
import com.jienoshiri.platform.mapper.PostLikeMapper;
import com.jienoshiri.platform.mapper.PostMapper;
import com.jienoshiri.platform.mapper.SysConfigMapper;
import com.jienoshiri.platform.mapper.UserMapper;
import com.jienoshiri.platform.utils.SensitiveWordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostMapper postMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PostLikeMapper postLikeMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private RecommendationService recommendationService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private SensitiveWordUtil sensitiveWordUtil;
    @Mock
    private SysConfigMapper sysConfigMapper;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService();
        ReflectionTestUtils.setField(postService, "postMapper", postMapper);
        ReflectionTestUtils.setField(postService, "userMapper", userMapper);
        ReflectionTestUtils.setField(postService, "postLikeMapper", postLikeMapper);
        ReflectionTestUtils.setField(postService, "commentMapper", commentMapper);
        ReflectionTestUtils.setField(postService, "recommendationService", recommendationService);
        ReflectionTestUtils.setField(postService, "notificationService", notificationService);
        ReflectionTestUtils.setField(postService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(postService, "sensitiveWordUtil", sensitiveWordUtil);
        ReflectionTestUtils.setField(postService, "sysConfigMapper", sysConfigMapper);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any())).thenReturn(null);
        when(postLikeMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
    }

    @Test
    void getPostList_shouldIncludeStatus3AndPromoteByWikiWeight() {
        Post normalPost = buildPost(100L, 10L, 1, "normal");
        Post wikiPost = buildPost(99L, 11L, 3, "wiki");

        when(postMapper.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList(normalPost, wikiPost));

        SysUser normalAuthor = new SysUser();
        normalAuthor.setId(10L);
        normalAuthor.setNickname("author-1");
        normalAuthor.setReputation(0);

        SysUser wikiAuthor = new SysUser();
        wikiAuthor.setId(11L);
        wikiAuthor.setNickname("author-2");
        wikiAuthor.setReputation(0);

        when(userMapper.selectById(10L)).thenReturn(normalAuthor);
        when(userMapper.selectById(11L)).thenReturn(wikiAuthor);

        List<PostVo> result = postService.getPostList(null, null, null, null, null);

        assertEquals(2, result.size());
        assertEquals(99L, result.get(0).getId());
        assertTrue(result.get(0).getScore() > result.get(1).getScore());

        ArgumentCaptor<QueryWrapper<Post>> queryCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(postMapper).selectList(queryCaptor.capture());
        String sqlSegment = queryCaptor.getValue().getSqlSegment().toLowerCase();
        assertTrue(sqlSegment.contains("status"));
        assertTrue(sqlSegment.contains("in"));
    }

    private Post buildPost(Long id, Long userId, Integer status, String title) {
        Post post = new Post();
        post.setId(id);
        post.setUserId(userId);
        post.setStatus(status);
        post.setTitle(title);
        post.setContent("content");
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        return post;
    }
}
