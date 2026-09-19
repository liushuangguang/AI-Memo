package com.newtech.note.service;

import com.newtech.note.entity.request.SearchDictRequest;
import com.newtech.note.entity.vo.DictEntryList;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DictService {
    Mono<List<DictEntryList>> listDict(SearchDictRequest request);
}
