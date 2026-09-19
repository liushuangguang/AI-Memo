package com.newtech.note.aspect;

import com.newtech.note.service.NoteEmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EmbeddingAspectConditionalTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(NoteEmbeddingService.class, () -> mock(NoteEmbeddingService.class))
            .withUserConfiguration(EmbeddingAspect.class);

    @Test
    void isDisabledUnlessMilvusIsExplicitlyEnabled() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(EmbeddingAspect.class));
    }

    @Test
    void isCreatedWhenMilvusIsEnabled() {
        contextRunner.withPropertyValues("milvus.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(EmbeddingAspect.class));
    }
}
