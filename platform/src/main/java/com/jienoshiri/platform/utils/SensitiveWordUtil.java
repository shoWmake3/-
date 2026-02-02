package com.jienoshiri.platform.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SensitiveWordUtil {

    // 敏感词库 (DFA结构)
    private static Map sensitiveWordMap = null;

    // 最小匹配规则
    public static int minMatchTYpe = 1;
    // 最大匹配规则
    public static int maxMatchType = 2;

    /**
     * 初始化敏感词库
     * 实际项目中通常从数据库或txt文件加载，这里为了演示直接硬编码一部分
     */
    @PostConstruct
    public void initKeyWord() {
        Set<String> keyWordSet = new HashSet<>();
        // TODO: 可以在这里添加更多词汇，或者从数据库加载
        keyWordSet.add("笨蛋");
        keyWordSet.add("傻瓜");
        keyWordSet.add("骗子");
        keyWordSet.add("黑中介");
        keyWordSet.add("作弊");
        keyWordSet.add("代写");
        addSensitiveWordToHashMap(keyWordSet);
    }

    /**
     * 将敏感词集合添加到HashMap中，构建DFA算法模型
     */
    private void addSensitiveWordToHashMap(Set<String> keyWordSet) {
        sensitiveWordMap = new HashMap(keyWordSet.size());
        String key = null;
        Map nowMap = null;
        Map<String, String> newWorMap = null;
        Iterator<String> iterator = keyWordSet.iterator();
        while (iterator.hasNext()) {
            key = iterator.next();
            nowMap = sensitiveWordMap;
            for (int i = 0; i < key.length(); i++) {
                char keyChar = key.charAt(i);
                Object wordMap = nowMap.get(keyChar);
                if (wordMap != null) {
                    nowMap = (Map) wordMap;
                } else {
                    newWorMap = new HashMap<String, String>();
                    newWorMap.put("isEnd", "0");
                    nowMap.put(keyChar, newWorMap);
                    nowMap = newWorMap;
                }
                if (i == key.length() - 1) {
                    nowMap.put("isEnd", "1");
                }
            }
        }
    }

    /**
     * 检查文字中是否包含敏感字符
     */
    public boolean isContaintSensitiveWord(String txt, int matchType) {
        boolean flag = false;
        for (int i = 0; i < txt.length(); i++) {
            int matchFlag = checkSensitiveWord(txt, i, matchType);
            if (matchFlag > 0) {
                flag = true;
            }
        }
        return flag;
    }

    /**
     * 检查文字中的敏感词，如果有，用 * 替换
     */
    public String replaceSensitiveWord(String txt, int matchType, String replaceChar) {
        String resultTxt = txt;
        Set<String> set = getSensitiveWord(txt, matchType);
        Iterator<String> iterator = set.iterator();
        String word = null;
        String replaceString = null;
        while (iterator.hasNext()) {
            word = iterator.next();
            replaceString = getReplaceChars(replaceChar, word.length());
            resultTxt = resultTxt.replaceAll(word, replaceString);
        }
        return resultTxt;
    }

    private Set<String> getSensitiveWord(String txt, int matchType) {
        Set<String> sensitiveWordList = new HashSet<String>();
        for (int i = 0; i < txt.length(); i++) {
            int length = checkSensitiveWord(txt, i, matchType);
            if (length > 0) {
                sensitiveWordList.add(txt.substring(i, i + length));
                i = i + length - 1;
            }
        }
        return sensitiveWordList;
    }

    private String getReplaceChars(String replaceChar, int length) {
        String resultReplace = replaceChar;
        for (int i = 1; i < length; i++) {
            resultReplace += replaceChar;
        }
        return resultReplace;
    }

    private int checkSensitiveWord(String txt, int beginIndex, int matchType) {
        boolean flag = false;
        int matchFlag = 0;
        char word = 0;
        Map nowMap = sensitiveWordMap;
        for (int i = beginIndex; i < txt.length(); i++) {
            word = txt.charAt(i);
            nowMap = (Map) nowMap.get(word);
            if (nowMap != null) {
                matchFlag++;
                if ("1".equals(nowMap.get("isEnd"))) {
                    flag = true;
                    if (SensitiveWordUtil.minMatchTYpe == matchType) {
                        break;
                    }
                }
            } else {
                break;
            }
        }
        if (matchFlag < 2 || !flag) {
            matchFlag = 0;
        }
        return matchFlag;
    }
}