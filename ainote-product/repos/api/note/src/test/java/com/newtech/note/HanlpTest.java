package com.newtech.note;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import io.micrometer.common.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class HanlpTest {

    public static void main(String[] args) {
        String text = "Detailed analysis results for analysis 1 go here.我是新内容你觉得怎么样\n" +
                "注意天气，带好雨衣或雨伞\n" +
                "今天不想干活\n" +
                "今天不想干活\n" +
                "睡觉";
        List<Term> termList = HanLP.segment(text);
        String str = termList.stream().map(term -> term.word).filter(StringUtils::isNotBlank).collect(Collectors.joining(" "));
        System.out.println(str);
    }
}
