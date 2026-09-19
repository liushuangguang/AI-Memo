package com.newtech.note.service.impl;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.enumeration.NoteType;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.dto.noteModules.KeyValueModule;
import com.newtech.note.entity.dto.noteModules.NoteModule;
import com.newtech.note.entity.request.CreateNoteRequest;
import com.newtech.note.repositories.NoteRepository;
import com.newtech.note.service.NoteServiceV2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Service
public class NoteCategorizeService {

    private final NoteServiceV2 noteService;

    private final NoteRepository repository;


    public NoteCategorizeService(NoteServiceV2 noteService, NoteRepository repository) {
        this.noteService = noteService;
        this.repository = repository;
    }

    /**
     * 保存分类后的笔记
     *
     * @param sourceNote 当前正在分析归类的笔记
     * @param noteType   这个笔记（其中一部分）被归为哪一类
     * @param newModules 归类后的系统级别的笔记需要添加的新模块
     * @param systemNote 当前已经存在的系统级别的该类型的笔记
     * @return 归类后系统级别的笔记
     */

    public Mono<Note> saveEachCategorizedNote(Note sourceNote, NoteType noteType, List<NoteModule> newModules, Note systemNote) {
        if (CollectionUtils.isEmpty(newModules)) {
            return Mono.empty();
        }
        if (systemNote == null) {
            CreateNoteRequest createNoteRequest = new CreateNoteRequest();
            createNoteRequest.setTitle("系统笔记：" + noteType.getNoteTypeName());
            createNoteRequest.setDimension(1);
            createNoteRequest.setNoteType(noteType.getNoteType());
            createNoteRequest.setModuleList(newModules);
            return noteService.createNote(sourceNote.getDeviceId(), createNoteRequest)
                    .map(NoteBaseResponse::getData);
        }
        return repository.findById(systemNote.getId())
                .flatMap(oldNote -> {
                    oldNote.setModules(Stream.concat(oldNote.getModules().stream().filter(module -> {
                        if (module instanceof KeyValueModule keyValueModule) {
                            //如果存在且和当前note id相等则删除，之后会加入新的module
                            return !keyValueModule.getSourceNoteId().equals(sourceNote.getId());
                        }
                        return true;
                    }), newModules.stream()).toList());
                    oldNote.setUpdatedAt(LocalDateTime.now());
                    return repository.save(oldNote);
                });
    }

}
