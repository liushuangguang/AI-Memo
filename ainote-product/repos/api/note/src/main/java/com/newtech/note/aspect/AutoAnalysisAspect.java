package com.newtech.note.aspect;

import com.newtech.note.annotation.AutoAnalysis;
import com.newtech.note.entity.dto.NoteAnalysis;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class AutoAnalysisAspect {
    private final RedisTemplate<String, String> redisTemplate;

    public AutoAnalysisAspect(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @AfterReturning(pointcut = "@annotation(autoAnalysis)", returning = "result")
    public void handleAutoAnalysis(JoinPoint joinPoint, AutoAnalysis autoAnalysis, Object result) {
        // 假设返回的结果是保存的数据对象
        if (result instanceof NoteAnalysis) {
            NoteAnalysis entity = (NoteAnalysis) result;
            long id = entity.getId();
            int delay = autoAnalysis.delay();
            // 将 ID 放入 Redis 延迟队列，设置过期时间
            redisTemplate.opsForValue().set(String.valueOf(id), "pending", delay, TimeUnit.SECONDS);
        }
    }
}
