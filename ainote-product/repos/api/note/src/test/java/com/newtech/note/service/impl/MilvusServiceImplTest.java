package com.newtech.note.service.impl;

import io.milvus.client.MilvusClient;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MilvusServiceImplTest {
    @Test
    void baseConfigExposesNonLocalMilvusEnvironmentContract() throws IOException {
        var propertySource = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"))
                .get(0);

        assertEquals("${MILVUS_HOST:}", propertySource.getProperty("milvus.host"));
        assertEquals("${MILVUS_PORT:19530}", propertySource.getProperty("milvus.port"));
    }

    @Test
    void nonLocalPropertyInitializesClientLazilyOnlyOnce() {
        TestMilvusService service = new TestMilvusService("milvus.internal");

        service.client();
        service.client();

        assertEquals(1, service.created.get());
    }

    @Test
    void springResolvesExplicitNonLocalMilvusProperties() {
        new ApplicationContextRunner()
                .withUserConfiguration(MilvusPropertyConfiguration.class)
                .withPropertyValues("milvus.host=milvus.service", "milvus.port=19531")
                .run(context -> {
                    TestMilvusService service = context.getBean(TestMilvusService.class);
                    assertEquals("milvus.service", service.configuredHost);
                    assertEquals(19531, service.configuredPort);
                    service.client();
                    assertEquals(1, service.created.get());
        });
    }

    @Test
    void independentRpcsCanEnterClientConcurrently() throws Exception {
        CountDownLatch bothEntered = new CountDownLatch(2);
        CountDownLatch releaseRpcs = new CountDownLatch(1);
        TestMilvusService service = new TestMilvusService("milvus.internal", bothEntered, releaseRpcs, null);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> first = executor.submit(() -> service.insertData(
                "notes", "uid-1", "note-1", "note", "theme", java.util.List.of(1.0f)).block());
        Future<?> second = executor.submit(() -> service.insertData(
                "notes", "uid-2", "note-2", "note", "theme", java.util.List.of(1.0f)).block());

        assertEquals(true, bothEntered.await(5, TimeUnit.SECONDS));
        releaseRpcs.countDown();
        for (Future<?> future : java.util.List.of(first, second)) {
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (java.util.concurrent.ExecutionException expected) {
                // The proxy deliberately returns no RPC response.
            }
        }
        executor.shutdown();
        assertEquals(true, executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Configuration(proxyBeanMethods = false)
    static class MilvusPropertyConfiguration {
        @Bean
        TestMilvusService milvusService(
                @Value("${milvus.host}") String host,
                @Value("${milvus.port}") int port) {
            return new TestMilvusService(host, port);
        }
    }

    @Test
    void closeBeforeInitializationDoesNothing() {
        TestMilvusService service = new TestMilvusService("127.0.0.1");
        service.close();
        assertEquals(0, service.created.get());
        assertEquals(0, service.closedCount.get());
    }

    @Test
    void concurrentFirstAccessCreatesOneClient() throws Exception {
        TestMilvusService service = new TestMilvusService("127.0.0.1");
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        java.util.List<Future<MilvusClient>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                service.client();
                return service.client();
            }));
        }
        start.countDown();
        MilvusClient expectedClient = service.client();
        for (Future<MilvusClient> future : futures) {
            assertSame(expectedClient, future.get(5, TimeUnit.SECONDS));
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        assertEquals(1, service.created.get());
    }

    @Test
    void closeWaitsForInFlightRpcBeforeClosingClient() throws Exception {
        CountDownLatch rpcStarted = new CountDownLatch(1);
        CountDownLatch releaseRpc = new CountDownLatch(1);
        CountDownLatch closeStarted = new CountDownLatch(1);
        TestMilvusService service = new TestMilvusService("milvus.internal", rpcStarted, releaseRpc, closeStarted);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<?> rpc = executor.submit(() -> service.insertData(
                "notes", "uid", "note-id", "note", "theme", java.util.List.of(1.0f)).block());
        assertEquals(true, rpcStarted.await(5, TimeUnit.SECONDS));
        Future<?> closing = executor.submit(service::close);

        assertEquals(true, closeStarted.await(5, TimeUnit.SECONDS));
        assertEquals(0, service.closedCount.get());
        assertEquals(false, closing.isDone());

        releaseRpc.countDown();
        try {
            rpc.get(5, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException expected) {
            // The proxy deliberately returns no RPC response; the lease must still be released.
        }
        closing.get(5, TimeUnit.SECONDS);
        assertEquals(1, service.closedCount.get());
        assertThrows(IllegalStateException.class, service::client);
        executor.shutdown();
    }

    @Test
    void concurrentCloseCallsCloseClientOnlyOnce() throws Exception {
        CountDownLatch rpcStarted = new CountDownLatch(1);
        CountDownLatch releaseRpc = new CountDownLatch(1);
        CountDownLatch closeStarted = new CountDownLatch(1);
        TestMilvusService service = new TestMilvusService("milvus.internal", rpcStarted, releaseRpc, closeStarted);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        Future<?> rpc = executor.submit(() -> service.insertData(
                "notes", "uid", "note-id", "note", "theme", java.util.List.of(1.0f)).block());
        assertEquals(true, rpcStarted.await(5, TimeUnit.SECONDS));
        Future<?> firstClose = executor.submit(service::close);
        assertEquals(true, closeStarted.await(5, TimeUnit.SECONDS));
        Future<?> secondClose = executor.submit(service::close);

        releaseRpc.countDown();
        try {
            rpc.get(5, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException expected) {
            // The proxy deliberately returns no RPC response.
        }
        firstClose.get(5, TimeUnit.SECONDS);
        secondClose.get(5, TimeUnit.SECONDS);
        assertEquals(1, service.closedCount.get());
        executor.shutdown();
    }

    @Test
    void interruptedClosePublishesClosedEvenWhenClientCloseFails() throws Exception {
        CountDownLatch rpcStarted = new CountDownLatch(1);
        CountDownLatch releaseRpc = new CountDownLatch(1);
        CountDownLatch closeStarted = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        TestMilvusService service = new TestMilvusService(
                "milvus.internal", rpcStarted, releaseRpc, closeStarted, true);
        Thread rpc = new Thread(() -> service.insertData(
                "notes", "uid", "note-id", "note", "theme", java.util.List.of(1.0f)).block());
        rpc.start();
        assertEquals(true, rpcStarted.await(5, TimeUnit.SECONDS));
        Thread closer = new Thread(() -> {
            try {
                service.close();
            } catch (RuntimeException expected) {
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });
        closer.start();
        assertEquals(true, closeStarted.await(5, TimeUnit.SECONDS));
        closer.interrupt();
        releaseRpc.countDown();
        closer.join(5000);
        rpc.join(5000);

        assertEquals(true, interrupted.get());
        assertEquals(1, service.closedCount.get());
        assertThrows(IllegalStateException.class, service::client);
    }

    @Test
    void initializedClientClosesOnlyOnce() {
        TestMilvusService service = new TestMilvusService("127.0.0.1");
        service.client();
        service.close();
        service.close();
        assertEquals(1, service.closedCount.get());
        assertThrows(IllegalStateException.class, service::client);
    }

    @Test
    void missingHostFailsClearlyOnFirstAccess() {
        TestMilvusService service = new TestMilvusService("");
        IllegalStateException error = assertThrows(IllegalStateException.class, service::client);
        assertEquals("Milvus host is not configured; set MILVUS_HOST or milvus.host before using Milvus", error.getMessage());
        assertEquals(0, service.created.get());
    }

    @Test
    void closedServiceCannotRecreateClient() {
        TestMilvusService service = new TestMilvusService("127.0.0.1");
        service.close();
        assertThrows(IllegalStateException.class, service::client);
        assertEquals(0, service.created.get());
    }

    private static final class TestMilvusService extends MilvusServiceImpl {
        private final AtomicInteger created = new AtomicInteger();
        private final AtomicInteger closedCount = new AtomicInteger();
        private final CountDownLatch rpcStarted;
        private final CountDownLatch releaseRpc;
        private final CountDownLatch closeStarted;
        private final boolean closeFails;
        private final String configuredHost;
        private final int configuredPort;

        private TestMilvusService(String host) {
            this(host, 19530);
        }

        private TestMilvusService(String host, int port) {
            this(host, null, null, null, false, host, port);
        }

        private TestMilvusService(String host, CountDownLatch rpcStarted, CountDownLatch releaseRpc) {
            this(host, rpcStarted, releaseRpc, null);
        }

        private TestMilvusService(String host, CountDownLatch rpcStarted, CountDownLatch releaseRpc,
                                  CountDownLatch closeStarted) {
            this(host, rpcStarted, releaseRpc, closeStarted, false);
        }

        private TestMilvusService(String host, CountDownLatch rpcStarted, CountDownLatch releaseRpc,
                                  CountDownLatch closeStarted, boolean closeFails) {
            this(host, rpcStarted, releaseRpc, closeStarted, closeFails, host, 19530);
        }

        private TestMilvusService(String host, CountDownLatch rpcStarted, CountDownLatch releaseRpc,
                                  CountDownLatch closeStarted, boolean closeFails,
                                  String configuredHost, int configuredPort) {
            super(host, configuredPort);
            this.rpcStarted = rpcStarted;
            this.releaseRpc = releaseRpc;
            this.closeStarted = closeStarted;
            this.closeFails = closeFails;
            this.configuredHost = configuredHost;
            this.configuredPort = configuredPort;
        }

        @Override
        public void close() {
            if (closeStarted != null) {
                closeStarted.countDown();
            }
            super.close();
        }

        private MilvusClient client() {
            return getMilvusClient();
        }

        @Override
        protected MilvusClient createMilvusClient() {
            created.incrementAndGet();
            return (MilvusClient) Proxy.newProxyInstance(
                    MilvusClient.class.getClassLoader(),
                    new Class<?>[]{MilvusClient.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("insert") && rpcStarted != null) {
                            rpcStarted.countDown();
                            releaseRpc.await();
                        }
                        if (method.getName().equals("close") && method.getParameterCount() == 0) {
                            closedCount.incrementAndGet();
                            if (closeFails) {
                                throw new IllegalStateException("close failed");
                            }
                        }
                        return null;
                    });
        }
    }
}
