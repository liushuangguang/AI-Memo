package com.newtech.note.util;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SegmentUtil {
    public static String segment(String... contents) {
        String combinedContent = Arrays.stream(contents)
                .filter(StringUtils::isNotBlank)
                .map(NoteDeltaJsonParser::parse)
                .collect(Collectors.joining("\n"));

        List<Term> termList = HanLP.segment(combinedContent);
        return termList.stream()
                .map(term -> term.word)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(" "));
    }
}
