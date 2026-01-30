package com.jienoshiri.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class TranslationService {

    // ⭐ 必填：你的 APP ID 和 密钥
    private static final String APP_ID = "20260129002549415";
    private static final String SECURITY_KEY = "OwaR5pSx9dB7xGZ6MDBy";

    private static final String URL = "http://api.fanyi.baidu.com/api/trans/vip/translate";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public String translate(String content, String targetLang) {
        // 兼容性处理：如果你传了 ja，自动转成百度要求的 jp
        if ("ja".equals(targetLang)) {
            targetLang = "jp";
        }

        System.out.println(">>> 准备翻译: " + content + " -> " + targetLang);

        if (APP_ID.isEmpty() || SECURITY_KEY.isEmpty()) {
            return mockTranslate(content, targetLang);
        }

        try {
            return callBaiduApi(content, targetLang);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(">>> 异常: " + e.getMessage());
            return mockTranslate(content, targetLang);
        }
    }

    private String callBaiduApi(String query, String to) throws Exception {
        // 1. 生成随机数
        String salt = String.valueOf(System.currentTimeMillis() + new Random().nextInt(1000));

        // 2. 生成签名：MD5(appid + q + salt + key)
        // 注意：这里的 query 必须是【原文】，绝对不能编码！
        String src = APP_ID + query + salt + SECURITY_KEY;
        String sign = DigestUtils.md5DigestAsHex(src.getBytes(StandardCharsets.UTF_8));

        // 3. ⭐ 核心修正：使用 Map 传参，让 RestTemplate 自动处理编码，防止双重编码
        // url 中使用占位符 {key}
        String requestUrl = URL + "?q={q}&from=auto&to={to}&appid={appid}&salt={salt}&sign={sign}";

        Map<String, String> params = new HashMap<>();
        params.put("q", query);      // 放原文，RestTemplate 会自动编码成 %E4%BD%A0...
        params.put("to", to);
        params.put("appid", APP_ID);
        params.put("salt", salt);
        params.put("sign", sign);

        System.out.println(">>> 请求参数: " + params);

        // 4. 发送请求
        String jsonResponse = restTemplate.getForObject(requestUrl, String.class, params);
        System.out.println(">>> 百度返回: " + jsonResponse);

        // 5. 解析结果
        JsonNode root = objectMapper.readTree(jsonResponse);

        if (root.has("error_code")) {
            String code = root.get("error_code").asText();
            String msg = root.get("error_msg").asText();
            // 54001: 签名错误; 52003: 未授权(Key错); 58000: 客户端IP受限
            System.err.println(">>> 百度API报错: " + code + " - " + msg);
            return "翻译错(Code:" + code + "):" + msg;
        }

        if (root.has("trans_result")) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode node : root.get("trans_result")) {
                sb.append(node.get("dst").asText()).append("\n");
            }
            return sb.toString().trim();
        }

        return "未获取到结果";
    }

    private String mockTranslate(String content, String targetLang) {
        // ... (保持之前的模拟逻辑不变)
        if ("jp".equals(targetLang) || "ja".equals(targetLang)) {
            return "【模拟日】" + content.replace("日本", "日本(Nihon)");
        }
        return "【模拟中】" + content.replace("Nihon", "日本");
    }
}