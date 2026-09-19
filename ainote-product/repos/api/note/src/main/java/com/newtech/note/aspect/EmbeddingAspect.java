package com.newtech.note.aspect;

import com.newtech.note.entity.dto.Note;
import com.newtech.note.service.NoteEmbeddingService;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Aspect
@Component
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "true")
public class EmbeddingAspect {
    private final NoteEmbeddingService noteEmbeddingService;

    public EmbeddingAspect(NoteEmbeddingService noteEmbeddingService) {
        this.noteEmbeddingService = noteEmbeddingService;
    }

    // 定义切点，拦截所有 ReactiveCrudRepository 的 save() 方法
    @Pointcut("execution(* org.springframework.data.repository.reactive.ReactiveCrudRepository+.save(..))")
    public void saveMethod() {
    }

    // 在 save() 方法执行之前触发异步操作
    @After("saveMethod()")
    public void beforeSave(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (ArrayUtils.isEmpty(args)) {
            return;
        }
        Object arg = args[0];
        // 如果参数是实体类型
        if (arg instanceof Note savedNote) {
            // 在更新 Milvus 数据库时传递 noteId
            noteEmbeddingService.embed(savedNote.getId()).subscribe();
        }
    }
}
