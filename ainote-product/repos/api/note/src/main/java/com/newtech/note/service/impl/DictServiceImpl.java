package com.newtech.note.service.impl;

import com.newtech.note.common.enumeration.NoteAnalysisType;
import com.newtech.note.common.enumeration.NoteType;
import com.newtech.note.common.enumeration.ProductPlatform;
import com.newtech.note.entity.request.SearchDictRequest;
import com.newtech.note.entity.vo.DictEntryList;
import com.newtech.note.service.DictService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class DictServiceImpl implements DictService {
    private static final Map<String, List<Pair<String, String>>> dictEntryListMap = Map.of(
            "noteType", NoteType.getDictEntries(),
            "shareUrl", List.of(Pair.of("noteDetail", "https://ainote.ai-tools.cloud/h5/share.html")),
            "noteAnalysisType", NoteAnalysisType.getDictEntries(),
            "productPlatform", ProductPlatform.getDictEntries());

    @Override
    public Mono<List<DictEntryList>> listDict(SearchDictRequest request) {
        if (request.getTypeNames().isEmpty()) {
            return Mono.just(List.of());
        }
        List<DictEntryList> dict = request.getTypeNames().stream().filter(dictEntryListMap::containsKey).map(typeName -> new DictEntryList(typeName, dictEntryListMap.get(typeName))).toList();
        return Mono.just(dict);
    }
}
