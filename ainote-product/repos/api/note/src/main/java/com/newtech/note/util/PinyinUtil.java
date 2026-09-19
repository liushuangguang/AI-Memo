package com.newtech.note.util;

import net.sourceforge.pinyin4j.PinyinHelper;

public class PinyinUtil {
    public static String chineseToPinyin(String chineseText) {
        StringBuilder pinyinBuilder = new StringBuilder();
        char[] chars = chineseText.toCharArray();
        for (char c : chars) {
            // 使用 PinyinHelper 拼音工具类将中文字符转换为拼音，非中文字符保持原样
            if (c == ' ') {
                pinyinBuilder.append(" ");
            } else if (c == '（') {
                // 声调数字 1 2 3 4 对应中文的声调
                pinyinBuilder.append("5");
            } else if (c == '）') {
                pinyinBuilder.append("6");
            } else {
                String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c);
                if (pinyinArray != null && pinyinArray.length > 0) {
                    // 只取第一个拼音，因为一个汉字可能有多个读音
                    pinyinBuilder.append(pinyinArray[0]);
                }
            }
        }
        return pinyinBuilder.toString().trim();
    }
}
