package com.newtech.note;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

public class PublisherTest {
    public static void performTimeConsumingTask() {
        // 计算密集型任务，执行大量的计算来消耗时间
        long start = System.nanoTime();

        // 计算密集型循环，进行大量无意义的运算，模拟 2 秒的延迟
        double result = 0.0;
        for (long j = 0; j < 4; j++) {
            for (long i = 0; i < 1_000_000_000; i++) {
                result += Math.sqrt(i);
            }
        }


        long end = System.nanoTime();
        long duration = end - start; // 纳秒
        System.out.println("threadName: " + Thread.currentThread().getName() +  "Task took: " + (duration / 1_000_000_000.0) + " seconds");
    }

    /**
     * doOnSuccess 如果要实现异步操作，则需要使用 subscribeOn() 方法切换线程，然后调用 subscribe() 方法订阅。
     * flatMap 虽然可以切换线程，但是不能实现异步操作，只能串行执行。
     * dispose() 用于取消订阅, 可能造成线程中断
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Mono<Object> objectMono = Mono.fromRunnable(() -> {
            //TimeUnit.SECONDS.sleep(5);
            performTimeConsumingTask();
            System.out.println("threadName: " + Thread.currentThread().getName() + "  sleep 10 seconds");
        });

        /*String ddd = Mono.just("ddd")
                .map(str -> {
                    System.out.println("threadName: " + Thread.currentThread().getName());
                    return str;
                })
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(response ->
                        objectMono.subscribeOn(Schedulers.boundedElastic()).subscribe()
                ).block();
        System.out.println("threadName: " + Thread.currentThread().getName() + "  value: " + ddd);*/

        /*String ddd = Mono.just("ddd")
                .map(str -> {
                    System.out.println("threadName: " + Thread.currentThread().getName());
                    return str;
                })
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(response ->
                        objectMono.subscribeOn(Schedulers.boundedElastic()).subscribe().dispose()
                ).block();
        System.out.println("threadName: " + Thread.currentThread().getName() + "  value: " + ddd);*/

        String ddd = Mono.just("ddd")
                .map(str -> {
                    System.out.println("threadName: " + Thread.currentThread().getName());
                    return str;
                })
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(response ->
                        performTimeConsumingTask()
                ).block();
        System.out.println("threadName: " + Thread.currentThread().getName() + "  value: " + ddd);


        //flatMap 虽然可以切换线程，但是不能实现异步操作，只能串行执行。
        /*String eee = Mono.just("eee")
                .map(str -> {
                    System.out.println("threadName: " + Thread.currentThread().getName());
                    return str;
                })
                .publishOn(Schedulers.boundedElastic())
                .flatMap(response ->
                        objectMono.subscribeOn(Schedulers.boundedElastic()).thenReturn(response)
                ).block();
        System.out.println("threadName: " + Thread.currentThread().getName() + "  value: " + eee);*/

        System.in.read();

    }
}
