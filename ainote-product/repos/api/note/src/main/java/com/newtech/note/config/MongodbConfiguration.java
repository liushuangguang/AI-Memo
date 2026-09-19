package com.newtech.note.config;

import com.newtech.note.entity.dto.Backlog;
import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.dto.NoteTheme;
import com.newtech.note.entity.dto.NoteAnalysisRecord;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Configuration
public class MongodbConfiguration {

    private final ReactiveMongoTemplate reactiveMongoTemplate;
    private final boolean indexesEnabled;

    public MongodbConfiguration(ReactiveMongoTemplate reactiveMongoTemplate,
                                @Value("${note.mongodb.indexes.enabled:true}") boolean indexesEnabled) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
        this.indexesEnabled = indexesEnabled;
    }

    @PostConstruct
    public void initIndexes() {
        if (!indexesEnabled) {
            return;
        }
        // 为 segmentation 字段创建文本索引
        reactiveMongoTemplate.indexOps(Note.class).ensureIndex(new TextIndexDefinition.TextIndexDefinitionBuilder()
                .onField("modules.segmentation")// module的segmentation字段
                .withDefaultLanguage("none")  // 设置 default_language 为 "none"，适用于中文
                .build()).subscribe();

        reactiveMongoTemplate.indexOps(Backlog.class).ensureIndex(new TextIndexDefinition.TextIndexDefinitionBuilder()
                .onField("segmentation")// module的segmentation字段
                .withDefaultLanguage("none")  // 设置 default_language 为 "none"，适用于中文
                .build()).subscribe();

        reactiveMongoTemplate.indexOps(NoteTheme.class).ensureIndex(new TextIndexDefinition.TextIndexDefinitionBuilder()
                .onField("segmentation")
                .withDefaultLanguage("none")  // 设置 default_language 为 "none"，适用于中文
                .build()).subscribe();

        Mono<String> noteVersionIndex = reactiveMongoTemplate.indexOps(NoteAnalysisRecord.class).ensureIndex(new Index()
                .on("noteId", Sort.Direction.ASC)
                .on("version", Sort.Direction.ASC)
                .unique()
                .named("uq_analysis_note_version"));

        Mono<String> snapshotIndex = reactiveMongoTemplate.indexOps(NoteAnalysisRecord.class).ensureIndex(new Index()
                .on("noteId", Sort.Direction.ASC)
                .on("snapshotHash", Sort.Direction.ASC)
                .unique()
                .partial(PartialIndexFilter.of(org.bson.Document.parse("{snapshotHash: {$exists: true}}")))
                .named("uq_analysis_note_snapshot"));

        Mono.when(noteVersionIndex, snapshotIndex).block(Duration.ofSeconds(30));
    }
}
