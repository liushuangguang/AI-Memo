package com.newtech.note;

import com.newtech.note.entity.dto.Note;
import com.newtech.note.entity.dto.UserInfo;
import com.newtech.note.entity.dto.noteModules.NoteModule;
import com.newtech.note.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SnowflakeIdGeneratorConcurrencyTest {
    private static final int THREAD_COUNT = 32;
    private static final int IDS_PER_THREAD = 4_000;

    @Test
    void generatesUniqueAndMonotonicIdsUnderConcurrency() throws Exception {
        SnowflakeIdGenerator generator = SnowflakeIdGenerator.getInstance();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<long[]>> futures = new ArrayList<>(THREAD_COUNT);

        try {
            for (int thread = 0; thread < THREAD_COUNT; thread++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent ID test did not start in time");
                    }
                    long[] generated = new long[IDS_PER_THREAD];
                    for (int index = 0; index < IDS_PER_THREAD; index++) {
                        Class<?> idType = switch (index % 3) {
                            case 0 -> Note.class;
                            case 1 -> NoteModule.class;
                            default -> UserInfo.class;
                        };
                        generated[index] = generator.nextId(idType);
                    }
                    return generated;
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            Set<Long> uniqueIds = new HashSet<>(THREAD_COUNT * IDS_PER_THREAD);
            for (Future<long[]> future : futures) {
                long[] generated = future.get(30, TimeUnit.SECONDS);
                for (int index = 0; index < generated.length; index++) {
                    uniqueIds.add(generated[index]);
                    if (index > 0) {
                        assertThat(generated[index])
                                .as("IDs observed by one caller must increase")
                                .isGreaterThan(generated[index - 1]);
                    }
                }
            }

            assertThat(uniqueIds).hasSize(THREAD_COUNT * IDS_PER_THREAD);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }
}
