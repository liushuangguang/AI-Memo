package com.newtech.note.config;

import com.newtech.note.client.BingSafeAddressResolverGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.resolver.dns.DnsServerAddressStreamProviders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.LoopResources;
import reactor.netty.transport.ProxyProvider;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

@Configuration
public class WebClientConfiguration {
    @Bean
    @Primary
    public WebClient webClient() {
        return WebClient.create();
    }

    @Bean(destroyMethod = "close")
    @Qualifier("bingAddressResolverGroup")
    public BingSafeAddressResolverGroup bingAddressResolverGroup() {
        DnsNameResolverBuilder builder = new DnsNameResolverBuilder()
                .channelType(NioDatagramChannel.class)
                .socketChannelType(NioSocketChannel.class)
                .nameServerProvider(DnsServerAddressStreamProviders.platformDefault())
                .completeOncePreferredResolved(false);
        return new BingSafeAddressResolverGroup(new DnsAddressResolverGroup(builder));
    }

    @Bean(destroyMethod = "dispose")
    @Qualifier("bingLoopResources")
    public LoopResources bingLoopResources() {
        return LoopResources.create("bing-http-nio");
    }

    @Bean
    @Qualifier("bingHttpClient")
    public HttpClient bingHttpClient(
            @Qualifier("bingAddressResolverGroup")
            BingSafeAddressResolverGroup resolverGroup,
            @Qualifier("bingLoopResources") LoopResources loopResources,
            @Value("${web-search.proxy-url:}") String proxyUrl) {
        HttpClient client = HttpClient.create().runOn(loopResources, false);
        Optional<LocalProxy> proxy = localHttpProxy(proxyUrl);
        if (proxyUrl != null && !proxyUrl.isBlank() && proxy.isEmpty()) {
            throw new IllegalArgumentException("web-search.proxy-url is invalid");
        }
        if (proxy.isPresent()) {
            LocalProxy endpoint = proxy.get();
            return client.proxy(spec -> spec.type(ProxyProvider.Proxy.HTTP)
                    .host(endpoint.host())
                    .port(endpoint.port()));
        }
        return resolverGroup.cancellationAware(client.resolver(resolverGroup));
    }

    @Bean
    @Qualifier("bingWebClient")
    public WebClient bingWebClient(@Qualifier("bingHttpClient") HttpClient httpClient) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    static Optional<LocalProxy> localHttpProxy(String configuredValue) {
        if (configuredValue == null || configuredValue.isBlank()) {
            return Optional.empty();
        }
        try {
            URI uri = URI.create(configuredValue.trim());
            String host = uri.getHost();
            String normalizedHost = host == null ? "" : host.toLowerCase(Locale.ROOT);
            if (normalizedHost.startsWith("[") && normalizedHost.endsWith("]")) {
                normalizedHost = normalizedHost.substring(1, normalizedHost.length() - 1);
            }
            boolean localHost = "localhost".equals(normalizedHost)
                    || "127.0.0.1".equals(normalizedHost)
                    || "::1".equals(normalizedHost);
            String path = uri.getRawPath();
            if (!"http".equalsIgnoreCase(uri.getScheme())
                    || !localHost
                    || uri.getPort() < 1 || uri.getPort() > 65535
                    || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null
                    || (path != null && !path.isBlank() && !"/".equals(path))) {
                return Optional.empty();
            }
            return Optional.of(new LocalProxy(normalizedHost, uri.getPort()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    record LocalProxy(String host, int port) {
    }
}
