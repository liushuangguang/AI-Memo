package com.newtech.note.controller;

import com.newtech.note.entity.request.SearchDictRequest;
import com.newtech.note.entity.vo.DictEntryList;
import com.newtech.note.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/dict")
@Tag(name = "字典处理器", description = "Note处理器，目前仅用于查询")
public class DictController {
    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @Operation(
            summary = "获得所有字典列表",
            description = "获得所有字典列表")
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public Mono<List<DictEntryList>> getDictList(@RequestHeader("device-id") String deviceId, @RequestBody SearchDictRequest request) {
        return dictService.listDict(request);
    }
}
