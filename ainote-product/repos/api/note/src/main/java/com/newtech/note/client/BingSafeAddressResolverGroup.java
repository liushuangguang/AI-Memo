package com.newtech.note.client;

import io.netty.resolver.AddressResolver;
import io.netty.resolver.AddressResolverGroup;
import io.netty.channel.Channel;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.netty.ReactorNetty;
import reactor.netty.http.client.HttpClient;
import reactor.util.context.ContextView;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Connection-boundary resolver for Bing that validates every resolved address before Netty can
 * connect. The validated {@link InetSocketAddress} instances are returned directly to the caller,
 * so the HTTP connection does not perform a second DNS lookup after validation.
 */
public final class BingSafeAddressResolverGroup extends AddressResolverGroup<InetSocketAddress> {
    /**
     * Complete address fields from the IANA IPv4 and IPv6 Special-Purpose Address Registries.
     * Snapshot: 2026-09-02.
     * https://www.iana.org/assignments/iana-ipv4-special-registry/iana-ipv4-special-registry.xml
     * https://www.iana.org/assignments/iana-ipv6-special-registry/iana-ipv6-special-registry.xml
     *
     * Entries intentionally remain explicit when covered by a broader registered prefix. This
     * makes registry reconciliation reviewable and prevents a newly narrowed parent entry from
     * silently opening a previously registered child range.
     */
    private static final List<String> IANA_SPECIAL_PURPOSE_CIDRS = List.of(
            "0.0.0.0/8",
            "0.0.0.0/32",
            "10.0.0.0/8",
            "100.64.0.0/10",
            "127.0.0.0/8",
            "169.254.0.0/16",
            "172.16.0.0/12",
            "192.0.0.0/24",
            "192.0.0.0/29",
            "192.0.0.8/32",
            "192.0.0.9/32",
            "192.0.0.10/32",
            "192.0.0.170/32",
            "192.0.0.171/32",
            "192.0.2.0/24",
            "192.31.196.0/24",
            "192.52.193.0/24",
            "192.88.99.0/24",
            "192.88.99.2/32",
            "192.168.0.0/16",
            "192.175.48.0/24",
            "198.18.0.0/15",
            "198.51.100.0/24",
            "203.0.113.0/24",
            "240.0.0.0/4",
            "255.255.255.255/32",
            "::1/128",
            "::/128",
            "::ffff:0:0/96",
            "64:ff9b::/96",
            "64:ff9b:1::/48",
            "100::/64",
            "100:0:0:1::/64",
            "2001::/23",
            "2001::/32",
            "2001:1::1/128",
            "2001:1::2/128",
            "2001:1::3/128",
            "2001:2::/48",
            "2001:3::/32",
            "2001:4:112::/48",
            "2001:10::/28",
            "2001:20::/28",
            "2001:30::/28",
            "2001:db8::/32",
            "2002::/16",
            "2620:4f:8000::/48",
            "3fff::/20",
            "5f00::/16",
            "fc00::/7",
            "fe80::/10");
    private static final List<IpPrefix> IANA_SPECIAL_PURPOSE_PREFIXES =
            IANA_SPECIAL_PURPOSE_CIDRS.stream().map(IpPrefix::parse).toList();
    private static final Object CANCELLATION_TOKEN_CONTEXT_KEY = new Object();

    private final AddressResolverGroup<InetSocketAddress> delegateGroup;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ThreadLocal<CancellationToken> resolvingRequest = new ThreadLocal<>();

    public BingSafeAddressResolverGroup(AddressResolverGroup<InetSocketAddress> delegateGroup) {
        this.delegateGroup = delegateGroup;
    }

    /** Propagates a cancelled HTTP connection acquisition to its exact in-flight DNS request. */
    public HttpClient cancellationAware(HttpClient client) {
        Objects.requireNonNull(client, "client");
        return client
                .doOnResolve(connection -> bindResolvingRequest(connection.channel()))
                .mapConnect(connectionMono -> Mono.defer(() -> {
                    CancellationToken token = new CancellationToken();
                    return connectionMono
                            .contextWrite(context -> context.put(
                                    CANCELLATION_TOKEN_CONTEXT_KEY, token))
                            .doFinally(signal -> {
                                if (signal == SignalType.CANCEL) {
                                    token.cancel();
                                }
                            });
                }));
    }

    private void bindResolvingRequest(Channel channel) {
        resolvingRequest.remove();
        ContextView context = ReactorNetty.getChannelContext(channel);
        if (context == null || !context.hasKey(CANCELLATION_TOKEN_CONTEXT_KEY)) {
            return;
        }
        CancellationToken token = context.get(CANCELLATION_TOKEN_CONTEXT_KEY);
        resolvingRequest.set(token);
        channel.eventLoop().execute(() -> {
            if (resolvingRequest.get() == token) {
                resolvingRequest.remove();
            }
        });
    }

    private CancellationToken takeResolvingRequest() {
        CancellationToken token = resolvingRequest.get();
        resolvingRequest.remove();
        return token;
    }

    @Override
    protected AddressResolver<InetSocketAddress> newResolver(EventExecutor executor) {
        AddressResolver<InetSocketAddress> delegate = delegateGroup.getResolver(executor);
        return new ValidatingResolver(delegate, executor);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                super.close();
            } finally {
                delegateGroup.close();
            }
        }
    }

    static boolean isPublicAddress(InetAddress address) {
        if (address == null
                || address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (!(address instanceof Inet4Address) && !(address instanceof Inet6Address)) {
            return false;
        }
        // IPv4-mapped IPv6 is itself an IANA special-purpose range. Reject the representation
        // rather than treating it as a second route to an otherwise public IPv4 destination.
        return !isIpv4Mapped(bytes)
                && IANA_SPECIAL_PURPOSE_PREFIXES.stream()
                .noneMatch(prefix -> prefix.matches(bytes));
    }

    private static boolean isIpv4Mapped(byte[] bytes) {
        if (bytes.length != 16 || bytes[10] != (byte) 0xff || bytes[11] != (byte) 0xff) {
            return false;
        }
        for (int index = 0; index < 10; index++) {
            if (bytes[index] != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBingHost(String host) {
        String normalized = host == null ? "" : host.toLowerCase(Locale.ROOT);
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return "bing.com".equals(normalized) || normalized.endsWith(".bing.com");
    }

    static List<String> ianaSpecialPurposeCidrs() {
        return IANA_SPECIAL_PURPOSE_CIDRS;
    }

    private record IpPrefix(byte[] bytes, int prefixLength) {
        private static IpPrefix parse(String cidr) {
            int separator = cidr.lastIndexOf('/');
            if (separator <= 0 || separator == cidr.length() - 1) {
                throw new IllegalArgumentException("Invalid special-purpose CIDR");
            }
            byte[] bytes = NetUtil.createByteArrayFromIpAddressString(
                    cidr.substring(0, separator));
            int prefixLength = Integer.parseInt(cidr.substring(separator + 1));
            if (bytes == null || prefixLength < 0 || prefixLength > bytes.length * Byte.SIZE) {
                throw new IllegalArgumentException("Invalid special-purpose CIDR");
            }
            return new IpPrefix(bytes, prefixLength);
        }

        private boolean matches(byte[] candidate) {
            if (candidate.length != bytes.length) {
                return false;
            }
            int completeBytes = prefixLength / Byte.SIZE;
            for (int index = 0; index < completeBytes; index++) {
                if (candidate[index] != bytes[index]) {
                    return false;
                }
            }
            int remainingBits = prefixLength % Byte.SIZE;
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xff << (Byte.SIZE - remainingBits);
            int expected = Byte.toUnsignedInt(bytes[completeBytes]);
            return (Byte.toUnsignedInt(candidate[completeBytes]) & mask)
                    == (expected & mask);
        }
    }

    private final class ValidatingResolver implements AddressResolver<InetSocketAddress> {
        private final AddressResolver<InetSocketAddress> delegate;
        private final EventExecutor executor;

        private ValidatingResolver(AddressResolver<InetSocketAddress> delegate,
                                   EventExecutor executor) {
            this.delegate = delegate;
            this.executor = executor;
        }

        @Override
        public boolean isSupported(SocketAddress address) {
            return address instanceof InetSocketAddress && delegate.isSupported(address);
        }

        @Override
        public boolean isResolved(SocketAddress address) {
            // A result returned by this resolver is already the fully validated connection target.
            // Mark it resolved so Netty connects directly instead of performing a second lookup.
            return address instanceof InetSocketAddress inetAddress
                    && !inetAddress.isUnresolved()
                    && inetAddress.getPort() == 443
                    && isPublicAddress(inetAddress.getAddress());
        }

        @Override
        public Future<InetSocketAddress> resolve(SocketAddress address) {
            return resolve(address, executor.newPromise());
        }

        @Override
        public Future<InetSocketAddress> resolve(SocketAddress address,
                                                 Promise<InetSocketAddress> promise) {
            Future<List<InetSocketAddress>> resolution = resolveAll(address);
            promise.addListener(ignored -> {
                if (promise.isCancelled()) {
                    resolution.cancel(false);
                }
            });
            resolution.addListener(ignored -> {
                if (resolution.isCancelled()) {
                    promise.cancel(false);
                    return;
                }
                if (!resolution.isSuccess()) {
                    promise.tryFailure(resolution.cause());
                    return;
                }
                List<InetSocketAddress> addresses = resolution.getNow();
                promise.trySuccess(addresses.getFirst());
            });
            return promise;
        }

        @Override
        public Future<List<InetSocketAddress>> resolveAll(SocketAddress address) {
            return resolveAll(address, executor.newPromise());
        }

        @Override
        public Future<List<InetSocketAddress>> resolveAll(
                SocketAddress address, Promise<List<InetSocketAddress>> promise) {
            CancellationToken cancellationToken = takeResolvingRequest();
            if (!(address instanceof InetSocketAddress inetAddress)
                    || inetAddress.getPort() != 443
                    || !isBingHost(inetAddress.getHostString())) {
                promise.tryFailure(new UnsafeBingAddressException(
                        "Only Bing HTTPS addresses may be resolved"));
                return promise;
            }
            Future<List<InetSocketAddress>> resolution = delegate.resolveAll(address);
            if (cancellationToken != null) {
                cancellationToken.attach(resolution, promise);
            }
            promise.addListener(ignored -> {
                if (promise.isCancelled()) {
                    resolution.cancel(false);
                }
            });
            resolution.addListener(ignored -> {
                if (resolution.isCancelled()) {
                    promise.cancel(false);
                    return;
                }
                if (!resolution.isSuccess()) {
                    promise.tryFailure(resolution.cause());
                    return;
                }
                List<InetSocketAddress> addresses = resolution.getNow();
                if (addresses == null || addresses.isEmpty()) {
                    promise.tryFailure(new UnknownHostException(inetAddress.getHostString()));
                    return;
                }
                boolean allPublic = addresses.stream()
                        .allMatch(resolved -> resolved.getPort() == 443
                                && isPublicAddress(resolved.getAddress()));
                if (!allPublic) {
                    promise.tryFailure(new UnsafeBingAddressException(
                            "Bing hostname resolved to a non-public address"));
                    return;
                }
                promise.trySuccess(List.copyOf(addresses));
            });
            return promise;
        }

        @Override
        public void close() {
            // The delegate group owns and shares its resolver lifecycle.
        }
    }

    private static final class CancellationToken {
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicReference<PendingResolution> pending = new AtomicReference<>();

        private void attach(Future<?> delegate, Promise<?> validated) {
            PendingResolution resolution = new PendingResolution(delegate, validated);
            if (!pending.compareAndSet(null, resolution)) {
                throw new IllegalStateException("Cancellation token already has a DNS request");
            }
            if (cancelled.get()) {
                resolution.cancel();
            }
        }

        private void cancel() {
            cancelled.set(true);
            PendingResolution resolution = pending.get();
            if (resolution != null) {
                resolution.cancel();
            }
        }
    }

    private record PendingResolution(Future<?> delegate, Promise<?> validated) {
        private void cancel() {
            validated.cancel(false);
            delegate.cancel(false);
        }
    }

    public static final class UnsafeBingAddressException extends SecurityException {
        public UnsafeBingAddressException(String message) {
            super(message);
        }
    }
}
