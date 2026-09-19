package com.newtech.note.config;

import com.newtech.note.client.BingSafeAddressResolverGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.LoopResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebClientConfigurationTest {

    @Test
    void acceptsOnlyCredentialFreeLoopbackHttpProxies() {
        assertThat(WebClientConfiguration.localHttpProxy("http://127.0.0.1:7890"))
                .contains(new WebClientConfiguration.LocalProxy("127.0.0.1", 7890));
        assertThat(WebClientConfiguration.localHttpProxy("http://localhost:8081/"))
                .contains(new WebClientConfiguration.LocalProxy("localhost", 8081));
        assertThat(WebClientConfiguration.localHttpProxy("http://[::1]:7890"))
                .contains(new WebClientConfiguration.LocalProxy("::1", 7890));

        assertThat(WebClientConfiguration.localHttpProxy("https://127.0.0.1:7890")).isEmpty();
        assertThat(WebClientConfiguration.localHttpProxy("http://user:pass@127.0.0.1:7890")).isEmpty();
        assertThat(WebClientConfiguration.localHttpProxy("http://192.168.1.2:7890")).isEmpty();
        assertThat(WebClientConfiguration.localHttpProxy("http://127.0.0.1")).isEmpty();
        assertThat(WebClientConfiguration.localHttpProxy("http://127.0.0.1:7890/path")).isEmpty();
        assertThat(WebClientConfiguration.localHttpProxy("not a URI")).isEmpty();
    }

    @Test
    void bingHttpAndDnsUseCompatibleDedicatedNioResourcesThatSpringDisposes() {
        LoopResources loopResources;
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(WebClientConfiguration.class)) {
            loopResources = context.getBean("bingLoopResources", LoopResources.class);
            HttpClient httpClient = context.getBean("bingHttpClient", HttpClient.class);
            BingSafeAddressResolverGroup resolverGroup = context.getBean(
                    "bingAddressResolverGroup", BingSafeAddressResolverGroup.class);

            assertThat(httpClient.configuration().loopResources()).isSameAs(loopResources);
            assertThat(httpClient.configuration().resolver()).isSameAs(resolverGroup);

            EventLoopGroup clientLoops = loopResources.onClient(false);
            assertThat(loopResources.onChannelClass(SocketChannel.class, clientLoops))
                    .isEqualTo(NioSocketChannel.class);
            assertThat(loopResources.onChannelClass(DatagramChannel.class, clientLoops))
                    .isEqualTo(NioDatagramChannel.class);
            assertThat(loopResources.isDisposed()).isFalse();
        }

        assertThat(loopResources.isDisposed()).isTrue();
    }

    @Test
    void explicitInvalidProxyFailsClosedInsteadOfSilentlyConnectingDirectly() {
        WebClientConfiguration configuration = new WebClientConfiguration();
        BingSafeAddressResolverGroup resolverGroup = configuration.bingAddressResolverGroup();
        LoopResources loopResources = configuration.bingLoopResources();
        try {
            assertThatThrownBy(() -> configuration.bingHttpClient(
                    resolverGroup, loopResources, "http://192.168.1.2:7890"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("web-search.proxy-url");
        } finally {
            resolverGroup.close();
            loopResources.dispose();
        }
    }
}
