package com.newtech.note.client;

import io.netty.resolver.AbstractAddressResolver;
import io.netty.resolver.AddressResolver;
import io.netty.resolver.AddressResolverGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.Disposable;
import reactor.netty.ConnectionObserver;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;
import reactor.test.StepVerifier;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BingSafeAddressResolverGroupTest {
    // Independent copy of the complete IANA XML address fields as of 2026-09-02.
    private static final List<String> EXPECTED_IANA_SPECIAL_PURPOSE_CIDRS = List.of(
            "0.0.0.0/8", "0.0.0.0/32", "10.0.0.0/8", "100.64.0.0/10",
            "127.0.0.0/8", "169.254.0.0/16", "172.16.0.0/12", "192.0.0.0/24",
            "192.0.0.0/29", "192.0.0.8/32", "192.0.0.9/32", "192.0.0.10/32",
            "192.0.0.170/32", "192.0.0.171/32", "192.0.2.0/24", "192.31.196.0/24",
            "192.52.193.0/24", "192.88.99.0/24", "192.88.99.2/32",
            "192.168.0.0/16", "192.175.48.0/24", "198.18.0.0/15",
            "198.51.100.0/24", "203.0.113.0/24", "240.0.0.0/4",
            "255.255.255.255/32", "::1/128", "::/128", "::ffff:0:0/96",
            "64:ff9b::/96", "64:ff9b:1::/48", "100::/64", "100:0:0:1::/64",
            "2001::/23", "2001::/32", "2001:1::1/128", "2001:1::2/128",
            "2001:1::3/128", "2001:2::/48", "2001:3::/32", "2001:4:112::/48",
            "2001:10::/28", "2001:20::/28", "2001:30::/28", "2001:db8::/32",
            "2002::/16", "2620:4f:8000::/48", "3fff::/20", "5f00::/16",
            "fc00::/7", "fe80::/10");

    @Test
    void productionRegistryMatchesCompleteIanaSnapshot() {
        assertThat(BingSafeAddressResolverGroup.ianaSpecialPurposeCidrs())
                .containsExactlyElementsOf(EXPECTED_IANA_SPECIAL_PURPOSE_CIDRS);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("ianaBoundaryAddresses")
    void rejectsFirstAndLastAddressOfEveryIanaPrefix(
            String cidr, String boundary, InetAddress address) {
        assertThat(BingSafeAddressResolverGroup.isPublicAddress(address))
                .as("%s %s", cidr, boundary)
                .isFalse();
    }

    @ParameterizedTest(name = "adjacent to {0} {1}")
    @MethodSource("ianaAdjacentAddresses")
    void classifiesEveryAdjacentAddressAgainstIndependentRegistrySnapshot(
            String cidr, String side, InetAddress address, boolean expectedPublic) {
        assertThat(BingSafeAddressResolverGroup.isPublicAddress(address))
                .as("%s %s", cidr, side)
                .isEqualTo(expectedPublic);
    }

    @ParameterizedTest
    @MethodSource("unsafeDnsAnswers")
    void rejectsAnyNonPublicDnsAnswer(String host, String address) throws Exception {
        StaticAddressResolverGroup delegate = new StaticAddressResolverGroup(address);
        BingSafeAddressResolverGroup safe = new BingSafeAddressResolverGroup(delegate);
        io.netty.channel.DefaultEventLoop eventLoop = new io.netty.channel.DefaultEventLoop();
        try {
            AddressResolver<InetSocketAddress> resolver = safe.getResolver(eventLoop);
            var resolution = resolver.resolveAll(
                    InetSocketAddress.createUnresolved(host, 443)).awaitUninterruptibly();

            assertThat(resolution.isSuccess()).isFalse();
            assertThat(resolution.cause())
                    .isInstanceOf(BingSafeAddressResolverGroup.UnsafeBingAddressException.class);
            assertThat(delegate.calls).hasValue(1);
        } finally {
            safe.close();
            eventLoop.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).syncUninterruptibly();
        }
    }

    private static Stream<Arguments> unsafeDnsAnswers() {
        return Stream.of(
                Arguments.of("www.bing.com", "0.255.255.255"),
                Arguments.of("www.bing.com", "127.0.0.1"),
                Arguments.of("cn.bing.com", "10.0.0.5"),
                Arguments.of("www.bing.com", "169.254.169.254"),
                Arguments.of("cn.bing.com", "100.64.0.1"),
                Arguments.of("www.bing.com", "172.31.255.255"),
                Arguments.of("www.bing.com", "192.0.0.255"),
                Arguments.of("www.bing.com", "192.0.2.0"),
                Arguments.of("www.bing.com", "192.0.2.255"),
                Arguments.of("www.bing.com", "192.31.196.1"),
                Arguments.of("www.bing.com", "192.52.193.1"),
                Arguments.of("www.bing.com", "192.88.99.1"),
                Arguments.of("www.bing.com", "192.168.255.255"),
                Arguments.of("www.bing.com", "198.18.0.0"),
                Arguments.of("www.bing.com", "198.19.255.255"),
                Arguments.of("www.bing.com", "198.51.100.255"),
                Arguments.of("www.bing.com", "203.0.113.255"),
                Arguments.of("www.bing.com", "224.0.0.0"),
                Arguments.of("www.bing.com", "240.0.0.0"),
                Arguments.of("www.bing.com", "255.255.255.255"),
                Arguments.of("www.bing.com", "::1"),
                Arguments.of("www.bing.com", "::ffff:0:1"),
                Arguments.of("www.bing.com", "64:ff9b::1"),
                Arguments.of("www.bing.com", "64:ff9b:1::1"),
                Arguments.of("www.bing.com", "100::1"),
                Arguments.of("www.bing.com", "2001::1"),
                Arguments.of("www.bing.com", "2001:2::1"),
                Arguments.of("www.bing.com", "2001:10::1"),
                Arguments.of("www.bing.com", "2001:20::1"),
                Arguments.of("www.bing.com", "2001:db8::"),
                Arguments.of("www.bing.com", "2001:db8:ffff:ffff:ffff:ffff:ffff:ffff"),
                Arguments.of("www.bing.com", "2002::1"),
                Arguments.of("www.bing.com", "3fff::1"),
                Arguments.of("www.bing.com", "fd00::1"),
                Arguments.of("cn.bing.com", "fe80::1"),
                Arguments.of("www.bing.com", "fec0::1"),
                Arguments.of("www.bing.com", "ff00::1"));
    }

    @Test
    void appliesIpv4PolicyToIpv4MappedIpv6Answers() throws Exception {
        assertThat(BingSafeAddressResolverGroup.isPublicAddress(ipv4Mapped("192.0.2.1")))
                .isFalse();
        assertThat(BingSafeAddressResolverGroup.isPublicAddress(ipv4Mapped("8.8.8.8")))
                .isFalse();
    }

    @Test
    void allowsPublicIpv4AndIpv6Answers() throws Exception {
        StaticAddressResolverGroup delegate = new StaticAddressResolverGroup(
                "8.8.8.8", "2001:4860:4860::8888");
        BingSafeAddressResolverGroup safe = new BingSafeAddressResolverGroup(delegate);
        io.netty.channel.DefaultEventLoop eventLoop = new io.netty.channel.DefaultEventLoop();
        try {
            AddressResolver<InetSocketAddress> resolver = safe.getResolver(eventLoop);
            var resolution = resolver.resolveAll(
                    InetSocketAddress.createUnresolved("cn.bing.com", 443))
                    .syncUninterruptibly();

            assertThat(resolution.getNow()).extracting(InetSocketAddress::getAddress)
                    .containsExactly(InetAddress.getByName("8.8.8.8"),
                            InetAddress.getByName("2001:4860:4860::8888"));
            assertThat(resolution.getNow()).allMatch(resolver::isResolved);
            assertThat(delegate.calls).hasValue(1);
        } finally {
            safe.close();
            eventLoop.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).syncUninterruptibly();
        }
    }

    @Test
    void rejectsWholeDnsSetWhenOnlyOneAnswerIsSpecialUse() throws Exception {
        StaticAddressResolverGroup delegate = new StaticAddressResolverGroup(
                "8.8.8.8", "198.51.100.7");
        BingSafeAddressResolverGroup safe = new BingSafeAddressResolverGroup(delegate);
        io.netty.channel.DefaultEventLoop eventLoop = new io.netty.channel.DefaultEventLoop();
        try {
            var resolution = safe.getResolver(eventLoop).resolveAll(
                    InetSocketAddress.createUnresolved("www.bing.com", 443))
                    .awaitUninterruptibly();

            assertThat(resolution.isSuccess()).isFalse();
            assertThat(resolution.cause())
                    .isInstanceOf(BingSafeAddressResolverGroup.UnsafeBingAddressException.class);
        } finally {
            safe.close();
            eventLoop.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).syncUninterruptibly();
        }
    }

    @Test
    void rejectsNonHttpsPortBeforeDnsLookup() throws Exception {
        StaticAddressResolverGroup delegate = new StaticAddressResolverGroup("8.8.8.8");
        BingSafeAddressResolverGroup safe = new BingSafeAddressResolverGroup(delegate);
        io.netty.channel.DefaultEventLoop eventLoop = new io.netty.channel.DefaultEventLoop();
        try {
            var resolution = safe.getResolver(eventLoop).resolveAll(
                    InetSocketAddress.createUnresolved("www.bing.com", 8443))
                    .awaitUninterruptibly();

            assertThat(resolution.isSuccess()).isFalse();
            assertThat(resolution.cause())
                    .isInstanceOf(BingSafeAddressResolverGroup.UnsafeBingAddressException.class);
            assertThat(delegate.calls).hasValue(0);
        } finally {
            safe.close();
            eventLoop.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).syncUninterruptibly();
        }
    }

    @Test
    void realHttpClientNeverConnectsWhenDnsSetContainsSpecialUseAnswer() {
        StaticAddressResolverGroup delegate = new StaticAddressResolverGroup(
                "8.8.8.8", "192.0.2.1");
        BingSafeAddressResolverGroup safe = new BingSafeAddressResolverGroup(delegate);
        AtomicInteger connected = new AtomicInteger();
        HttpClient httpClient = safe.cancellationAware(HttpClient.create()
                .resolver(safe))
                .observe((connection, state) -> {
                    if (state == ConnectionObserver.State.CONNECTED) {
                        connected.incrementAndGet();
                    }
                });
        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        BingRssSearchClient client = new BingRssSearchClient(
                webClient, "https://www.bing.com/search", 2, 2);

        StepVerifier.create(client.search("test"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(com.newtech.note.common.BusinessException.class);
                    assertThat(((com.newtech.note.common.BusinessException) error).getCode())
                            .isEqualTo("PUBLIC_SEARCH_ENDPOINT_INVALID");
                })
                .verify();
        assertThat(delegate.calls).hasValue(1);
        assertThat(connected).hasValue(0);
        safe.close();
    }

    private static Inet6Address ipv4Mapped(String ipv4) throws Exception {
        byte[] mapped = new byte[16];
        mapped[10] = (byte) 0xff;
        mapped[11] = (byte) 0xff;
        System.arraycopy(InetAddress.getByName(ipv4).getAddress(), 0, mapped, 12, 4);
        return Inet6Address.getByAddress(null, mapped, -1);
    }

    private static Stream<Arguments> ianaBoundaryAddresses() {
        return EXPECTED_IANA_SPECIAL_PURPOSE_CIDRS.stream().flatMap(cidr -> {
            TestPrefix prefix = TestPrefix.parse(cidr);
            return Stream.of(
                    Arguments.of(cidr, "first", prefix.firstAddress()),
                    Arguments.of(cidr, "last", prefix.lastAddress()));
        });
    }

    private static Stream<Arguments> ianaAdjacentAddresses() {
        return EXPECTED_IANA_SPECIAL_PURPOSE_CIDRS.stream().flatMap(cidr -> {
            TestPrefix prefix = TestPrefix.parse(cidr);
            Stream.Builder<Arguments> adjacent = Stream.builder();
            prefix.before().ifPresent(address -> adjacent.add(Arguments.of(
                    cidr, "before", address, expectedPublic(address))));
            prefix.after().ifPresent(address -> adjacent.add(Arguments.of(
                    cidr, "after", address, expectedPublic(address))));
            return adjacent.build();
        });
    }

    private static boolean expectedPublic(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        byte[] candidate = address.getAddress();
        if (candidate.length == 16 && candidate[10] == (byte) 0xff
                && candidate[11] == (byte) 0xff) {
            boolean mapped = true;
            for (int index = 0; index < 10; index++) {
                mapped &= candidate[index] == 0;
            }
            if (mapped) {
                return false;
            }
        }
        return EXPECTED_IANA_SPECIAL_PURPOSE_CIDRS.stream()
                .map(TestPrefix::parse)
                .noneMatch(prefix -> prefix.contains(address));
    }

    private record TestPrefix(BigInteger first, BigInteger last, int byteLength) {
        private static TestPrefix parse(String cidr) {
            int separator = cidr.lastIndexOf('/');
            byte[] address = io.netty.util.NetUtil.createByteArrayFromIpAddressString(
                    cidr.substring(0, separator));
            if (address == null) {
                throw new IllegalArgumentException(cidr);
            }
            int prefixLength = Integer.parseInt(cidr.substring(separator + 1));
            int hostBits = address.length * Byte.SIZE - prefixLength;
            BigInteger value = new BigInteger(1, address);
            BigInteger hostMask = BigInteger.ONE.shiftLeft(hostBits).subtract(BigInteger.ONE);
            BigInteger first = value.and(hostMask.not());
            return new TestPrefix(first, first.add(hostMask), address.length);
        }

        private InetAddress firstAddress() {
            return address(first);
        }

        private InetAddress lastAddress() {
            return address(last);
        }

        private java.util.Optional<InetAddress> before() {
            return first.signum() == 0
                    ? java.util.Optional.empty()
                    : java.util.Optional.of(address(first.subtract(BigInteger.ONE)));
        }

        private java.util.Optional<InetAddress> after() {
            BigInteger maximum = BigInteger.ONE.shiftLeft(byteLength * Byte.SIZE)
                    .subtract(BigInteger.ONE);
            return last.equals(maximum)
                    ? java.util.Optional.empty()
                    : java.util.Optional.of(address(last.add(BigInteger.ONE)));
        }

        private boolean contains(InetAddress address) {
            if (address.getAddress().length != byteLength) {
                return false;
            }
            BigInteger value = new BigInteger(1, address.getAddress());
            return value.compareTo(first) >= 0 && value.compareTo(last) <= 0;
        }

        private InetAddress address(BigInteger value) {
            byte[] raw = value.toByteArray();
            if (raw.length > byteLength) {
                raw = Arrays.copyOfRange(raw, raw.length - byteLength, raw.length);
            } else if (raw.length < byteLength) {
                byte[] padded = new byte[byteLength];
                System.arraycopy(raw, 0, padded, byteLength - raw.length, raw.length);
                raw = padded;
            }
            try {
                return InetAddress.getByAddress(raw);
            } catch (Exception impossible) {
                throw new AssertionError(impossible);
            }
        }
    }

    @Test
    void delayedDnsResolutionDoesNotBlockTheNettyEventLoop() throws Exception {
        DelayedAddressResolverGroup delegate = new DelayedAddressResolverGroup();
        BingSafeAddressResolverGroup safe = new BingSafeAddressResolverGroup(delegate);
        io.netty.channel.DefaultEventLoop eventLoop = new io.netty.channel.DefaultEventLoop();
        try {
            var resolution = safe.getResolver(eventLoop).resolveAll(
                    InetSocketAddress.createUnresolved("www.bing.com", 443));
            assertThat(delegate.started.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(resolution.isDone()).isFalse();

            CountDownLatch eventLoopMarker = new CountDownLatch(1);
            eventLoop.execute(eventLoopMarker::countDown);
            assertThat(eventLoopMarker.await(1, TimeUnit.SECONDS)).isTrue();

            assertThat(delegate.complete("8.8.8.8")).isTrue();
            assertThat(resolution.awaitUninterruptibly().isSuccess()).isTrue();
        } finally {
            safe.close();
            eventLoop.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).syncUninterruptibly();
        }
    }

    @Test
    void cancelledRequestNeverConnectsWhenDnsCompletesLater() throws Exception {
        DelayedAddressResolverGroup delegate = new DelayedAddressResolverGroup();
        BingSafeAddressResolverGroup safe = new BingSafeAddressResolverGroup(delegate);
        AtomicInteger connected = new AtomicInteger();
        CountDownLatch connectedLatch = new CountDownLatch(1);
        HttpClient httpClient = safe.cancellationAware(HttpClient.create()
                .resolver(safe))
                .observe((connection, state) -> {
                    if (state == ConnectionObserver.State.CONNECTED) {
                        connected.incrementAndGet();
                        connectedLatch.countDown();
                    }
                });
        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        BingRssSearchClient client = new BingRssSearchClient(
                webClient, "https://www.bing.com/search", 2, 2);

        StepVerifier.create(client.search("test"))
                .then(() -> {
                    try {
                        assertThat(delegate.started.await(1, TimeUnit.SECONDS)).isTrue();
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(interrupted);
                    }
                })
                .thenCancel()
                .verify();

        assertThat(delegate.cancelled.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(delegate.complete("8.8.8.8")).isFalse();
        assertThat(connectedLatch.await(500, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(connected).hasValue(0);
        safe.close();
    }

    @Test
    void concurrentRequestsOnOneEventLoopKeepCancellationAndReverseDnsCompletionIsolated()
            throws Exception {
        ConcurrentDelayedAddressResolverGroup delegate =
                new ConcurrentDelayedAddressResolverGroup();
        BingSafeAddressResolverGroup safe = new BingSafeAddressResolverGroup(delegate);
        LoopResources loops = LoopResources.create("bing-dns-isolation", 1, true);
        AtomicInteger connected = new AtomicInteger();
        HttpClient httpClient = safe.cancellationAware(HttpClient.newConnection()
                        .runOn(loops, false)
                        .resolver(safe))
                .observe((connection, state) -> {
                    if (state == ConnectionObserver.State.CONNECTED) {
                        connected.incrementAndGet();
                    }
                });
        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        BingRssSearchClient firstClient = new BingRssSearchClient(
                webClient, "https://www.bing.com/search", 2, 2);
        BingRssSearchClient secondClient = new BingRssSearchClient(
                webClient, "https://cn.bing.com/search", 2, 2);
        CountDownLatch secondFinished = new CountDownLatch(1);
        AtomicReference<Throwable> secondFailure = new AtomicReference<>();

        Disposable first = firstClient.search("first").subscribe(
                ignored -> { }, ignored -> { });
        Disposable second = secondClient.search("second").subscribe(
                ignored -> { }, failure -> {
                    secondFailure.set(failure);
                    secondFinished.countDown();
                });
        try {
            assertThat(delegate.awaitStarted("www.bing.com")).isTrue();
            assertThat(delegate.awaitStarted("cn.bing.com")).isTrue();

            // Finish the second lookup first. Its unsafe answer must fail only that request.
            assertThat(delegate.complete("cn.bing.com", "192.0.2.1")).isTrue();
            assertThat(secondFinished.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(secondFailure.get())
                    .isInstanceOf(com.newtech.note.common.BusinessException.class);
            assertThat(delegate.isCancelled("www.bing.com")).isFalse();

            first.dispose();
            assertThat(delegate.awaitCancelled("www.bing.com")).isTrue();
            assertThat(delegate.isCancelled("cn.bing.com")).isFalse();
            assertThat(delegate.complete("www.bing.com", "8.8.8.8")).isFalse();
            assertThat(connected).hasValue(0);
        } finally {
            first.dispose();
            second.dispose();
            safe.close();
            loops.disposeLater().block(Duration.ofSeconds(5));
        }
    }

    @Test
    void pooledConnectionLeavesNoStaleTokenForTheNextDnsCancellation() throws Exception {
        ConcurrentDelayedAddressResolverGroup delegate =
                new ConcurrentDelayedAddressResolverGroup();
        BingSafeAddressResolverGroup safe = new BingSafeAddressResolverGroup(delegate);
        LoopResources loops = LoopResources.create("bing-pool-token", 1, true);
        ConnectionProvider provider = ConnectionProvider.builder("bing-pool-token-test")
                .maxConnections(1)
                .pendingAcquireMaxCount(1)
                .build();
        DisposableServer server = HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .handle((request, response) -> response.sendString(Mono.just("ok")))
                .bindNow(Duration.ofSeconds(2));
        Set<String> connectedChannels = ConcurrentHashMap.newKeySet();
        HttpClient pooled = safe.cancellationAware(HttpClient.create(provider)
                        .runOn(loops, false))
                .observe((connection, state) -> {
                    if (state == ConnectionObserver.State.CONNECTED) {
                        connectedChannels.add(connection.channel().id().asLongText());
                    }
                });
        WebClient local = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(pooled))
                .baseUrl("http://localhost:" + server.port())
                .build();
        try {
            assertThat(local.get().retrieve().bodyToMono(String.class).block()).isEqualTo("ok");
            assertThat(local.get().retrieve().bodyToMono(String.class).block()).isEqualTo("ok");
            assertThat(connectedChannels).hasSize(1);

            HttpClient bingHttpClient = safe.cancellationAware(HttpClient.newConnection()
                    .runOn(loops, false)
                    .resolver(safe));
            WebClient bingWebClient = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(bingHttpClient))
                    .build();
            BingRssSearchClient bing = new BingRssSearchClient(
                    bingWebClient, "https://www.bing.com/search", 2, 2);
            Disposable request = bing.search("cancel").subscribe(
                    ignored -> { }, ignored -> { });
            assertThat(delegate.awaitStarted("www.bing.com")).isTrue();
            request.dispose();
            assertThat(delegate.awaitCancelled("www.bing.com")).isTrue();
        } finally {
            server.disposeNow(Duration.ofSeconds(2));
            provider.disposeLater().block(Duration.ofSeconds(5));
            safe.close();
            loops.disposeLater().block(Duration.ofSeconds(5));
        }
    }

    @Test
    void cancellingOuterResolutionPropagatesToDelegateFuture() throws Exception {
        DelayedAddressResolverGroup delegate = new DelayedAddressResolverGroup();
        BingSafeAddressResolverGroup safe = new BingSafeAddressResolverGroup(delegate);
        io.netty.channel.DefaultEventLoop eventLoop = new io.netty.channel.DefaultEventLoop();
        try {
            var resolution = safe.getResolver(eventLoop).resolveAll(
                    InetSocketAddress.createUnresolved("www.bing.com", 443));
            assertThat(delegate.started.await(1, TimeUnit.SECONDS)).isTrue();

            assertThat(resolution.cancel(false)).isTrue();
            assertThat(delegate.cancelled.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(delegate.complete("8.8.8.8")).isFalse();
        } finally {
            safe.close();
            eventLoop.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).syncUninterruptibly();
        }
    }

    @Test
    void resolverResourcesCloseExactlyOnce() {
        StaticAddressResolverGroup delegate = new StaticAddressResolverGroup("8.8.8.8");
        BingSafeAddressResolverGroup safe = new BingSafeAddressResolverGroup(delegate);

        safe.close();
        safe.close();

        assertThat(delegate.closeCalls).hasValue(1);
    }

    private static final class StaticAddressResolverGroup
            extends AddressResolverGroup<InetSocketAddress> {
        private final List<InetAddress> addresses;
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicInteger closeCalls = new AtomicInteger();

        private StaticAddressResolverGroup(String... addresses) {
            this.addresses = Stream.of(addresses).map(value -> {
                try {
                    return InetAddress.getByName(value);
                } catch (Exception failure) {
                    throw new IllegalArgumentException(failure);
                }
            }).toList();
        }

        @Override
        protected AddressResolver<InetSocketAddress> newResolver(EventExecutor executor) {
            return new AbstractAddressResolver<>(executor, InetSocketAddress.class) {
                @Override
                protected boolean doIsResolved(InetSocketAddress address) {
                    return false;
                }

                @Override
                protected void doResolve(InetSocketAddress unresolved,
                                         Promise<InetSocketAddress> promise) {
                    calls.incrementAndGet();
                    promise.setSuccess(new InetSocketAddress(
                            addresses.getFirst(), unresolved.getPort()));
                }

                @Override
                protected void doResolveAll(InetSocketAddress unresolved,
                                            Promise<List<InetSocketAddress>> promise) {
                    calls.incrementAndGet();
                    promise.setSuccess(addresses.stream()
                            .map(address -> new InetSocketAddress(address, unresolved.getPort()))
                            .toList());
                }
            };
        }

        @Override
        public void close() {
            closeCalls.incrementAndGet();
            super.close();
        }
    }

    private static final class DelayedAddressResolverGroup
            extends AddressResolverGroup<InetSocketAddress> {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch cancelled = new CountDownLatch(1);
        private final AtomicReference<Promise<List<InetSocketAddress>>> pending =
                new AtomicReference<>();

        @Override
        protected AddressResolver<InetSocketAddress> newResolver(EventExecutor executor) {
            return new AbstractAddressResolver<>(executor, InetSocketAddress.class) {
                @Override
                protected boolean doIsResolved(InetSocketAddress address) {
                    return false;
                }

                @Override
                protected void doResolve(InetSocketAddress unresolved,
                                         Promise<InetSocketAddress> promise) {
                    promise.setFailure(new UnsupportedOperationException("resolveAll expected"));
                }

                @Override
                protected void doResolveAll(InetSocketAddress unresolved,
                                            Promise<List<InetSocketAddress>> promise) {
                    pending.set(promise);
                    promise.addListener(ignored -> {
                        if (promise.isCancelled()) {
                            cancelled.countDown();
                        }
                    });
                    started.countDown();
                }
            };
        }

        private boolean complete(String address) throws Exception {
            Promise<List<InetSocketAddress>> promise = pending.get();
            return promise != null && promise.trySuccess(List.of(new InetSocketAddress(
                    InetAddress.getByName(address), 443)));
        }
    }

    private static final class ConcurrentDelayedAddressResolverGroup
            extends AddressResolverGroup<InetSocketAddress> {
        private final Map<String, Promise<List<InetSocketAddress>>> pending =
                new ConcurrentHashMap<>();
        private final Map<String, CountDownLatch> started = new ConcurrentHashMap<>();
        private final Map<String, CountDownLatch> cancelled = new ConcurrentHashMap<>();

        @Override
        protected AddressResolver<InetSocketAddress> newResolver(EventExecutor executor) {
            return new AbstractAddressResolver<>(executor, InetSocketAddress.class) {
                @Override
                protected boolean doIsResolved(InetSocketAddress address) {
                    return false;
                }

                @Override
                protected void doResolve(InetSocketAddress unresolved,
                                         Promise<InetSocketAddress> promise) {
                    promise.setFailure(new UnsupportedOperationException("resolveAll expected"));
                }

                @Override
                protected void doResolveAll(InetSocketAddress unresolved,
                                            Promise<List<InetSocketAddress>> promise) {
                    String host = unresolved.getHostString();
                    pending.put(host, promise);
                    promise.addListener(ignored -> {
                        if (promise.isCancelled()) {
                            cancelled.computeIfAbsent(host, unused -> new CountDownLatch(1))
                                    .countDown();
                        }
                    });
                    started.computeIfAbsent(host, unused -> new CountDownLatch(1)).countDown();
                }
            };
        }

        private boolean awaitStarted(String host) throws InterruptedException {
            return started.computeIfAbsent(host, unused -> new CountDownLatch(1))
                    .await(1, TimeUnit.SECONDS);
        }

        private boolean awaitCancelled(String host) throws InterruptedException {
            return cancelled.computeIfAbsent(host, unused -> new CountDownLatch(1))
                    .await(1, TimeUnit.SECONDS);
        }

        private boolean isCancelled(String host) {
            Promise<List<InetSocketAddress>> promise = pending.get(host);
            return promise != null && promise.isCancelled();
        }

        private boolean complete(String host, String address) throws Exception {
            Promise<List<InetSocketAddress>> promise = pending.get(host);
            return promise != null && promise.trySuccess(List.of(new InetSocketAddress(
                    InetAddress.getByName(address), 443)));
        }
    }
}
