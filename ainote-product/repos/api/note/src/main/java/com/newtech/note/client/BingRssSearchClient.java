package com.newtech.note.client;

import com.newtech.note.common.BusinessException;
import com.newtech.note.entity.search.WebPage;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Public, keyless web-search fallback backed by Bing's RSS endpoint. */
@Component
public class BingRssSearchClient {
    private static final Logger logger = LogManager.getLogger(BingRssSearchClient.class);
    private static final int MAX_REDIRECTS = 3;
    private final WebClient webClient;
    private final String endpoint;
    private final Duration timeout;
    private final int maxResults;

    public BingRssSearchClient(@Qualifier("bingWebClient") WebClient webClient,
                               @Value("${web-search.bing-rss-url:https://www.bing.com/search}") String endpoint,
                               @Value("${web-search.timeout-seconds:8}") long timeoutSeconds,
                               @Value("${web-search.max-results:8}") int maxResults) {
        this.webClient = webClient;
        this.endpoint = endpoint;
        this.timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.maxResults = Math.max(1, Math.min(maxResults, 20));
    }

    public Mono<List<WebPage>> search(String query) {
        if (StringUtils.isBlank(query)) {
            return Mono.error(new BusinessException("PUBLIC_SEARCH_QUERY_INVALID",
                    "Public web search requires a non-empty query"));
        }
        return Mono.defer(() -> {
            final URI initialUri;
            try {
                initialUri = UriComponentsBuilder.fromUriString(endpoint)
                                .queryParam("format", "rss")
                                .queryParam("q", query.trim())
                                .build()
                                .encode()
                                .toUri();
                validateBingUri(initialUri);
            } catch (RuntimeException failure) {
                logger.warn("Rejecting configured public search endpoint ({})",
                        safeEndpointDescription(endpoint));
                return Mono.error(new BusinessException("PUBLIC_SEARCH_ENDPOINT_INVALID",
                        "Public web search endpoint is not allowed"));
            }
            Set<String> visited = new HashSet<>();
            visited.add(canonicalUri(initialUri));
            return fetch(initialUri, 0, visited);
        })
                .timeout(timeout)
                .flatMap(xml -> Mono.fromCallable(() -> parse(xml))
                        .subscribeOn(Schedulers.boundedElastic()))
                .flatMap(results -> results.isEmpty()
                        ? Mono.error(new BusinessException("PUBLIC_SEARCH_EMPTY",
                        "Public web search returned no valid HTTP results"))
                        : Mono.just(results))
                .onErrorMap(failure -> failure instanceof BusinessException
                        ? failure
                        : new BusinessException(
                        hasCause(failure, BingSafeAddressResolverGroup.UnsafeBingAddressException.class)
                                ? "PUBLIC_SEARCH_ENDPOINT_INVALID"
                                : "PUBLIC_SEARCH_UNAVAILABLE",
                        "Public web search could not be completed"));
    }

    private Mono<String> fetch(URI uri, int redirectCount, Set<String> visited) {
        return webClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML)
                .exchangeToMono(response -> {
                    if (response.statusCode().is3xxRedirection()) {
                        String location = response.headers().header(HttpHeaders.LOCATION).stream()
                                .findFirst().orElse(null);
                        URI redirectUri = resolveSafeRedirect(uri, location);
                        if (!visited.add(canonicalUri(redirectUri))) {
                            return response.releaseBody().then(Mono.error(new BusinessException(
                                    "PUBLIC_SEARCH_REDIRECT_LOOP",
                                    "Public web search returned a redirect loop")));
                        }
                        if (redirectCount >= MAX_REDIRECTS) {
                            return response.releaseBody().then(Mono.error(new BusinessException(
                                    "PUBLIC_SEARCH_REDIRECT_LIMIT",
                                    "Public web search exceeded the redirect limit")));
                        }
                        return response.releaseBody().then(fetch(
                                redirectUri, redirectCount + 1, visited));
                    }
                    if (response.statusCode().isError()) {
                        return response.releaseBody().then(Mono.error(new BusinessException(
                                "PUBLIC_SEARCH_HTTP_ERROR",
                                "Public web search request failed")));
                    }
                    return response.bodyToMono(String.class);
                });
    }

    private URI resolveSafeRedirect(URI currentUri, String location) {
        if (StringUtils.isBlank(location)) {
            throw new BusinessException("PUBLIC_SEARCH_REDIRECT_INVALID",
                    "Public web search returned a redirect without a valid Location");
        }
        try {
            URI resolved = currentUri.resolve(location.trim());
            validateBingUri(resolved);
            return resolved;
        } catch (IllegalArgumentException failure) {
            throw new BusinessException("PUBLIC_SEARCH_REDIRECT_INVALID",
                    "Public web search returned an unsafe redirect");
        }
    }

    private void validateBingUri(URI uri) {
        String host = normalizeHost(uri.getHost());
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getUserInfo() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)
                || isLocalOrIpHost(host)
                || !isBingHost(host)) {
            throw new IllegalArgumentException("unsafe Bing URI");
        }
    }

    private String canonicalUri(URI uri) {
        URI normalized = uri.normalize();
        String host = normalizeHost(normalized.getHost());
        int port = normalized.getPort() == 443 ? -1 : normalized.getPort();
        String path = normalizeRawPath(normalized.getRawPath());
        StringBuilder canonical = new StringBuilder("https://").append(host);
        if (port >= 0) {
            canonical.append(':').append(port);
        }
        canonical.append(path);
        if (normalized.getRawQuery() != null) {
            canonical.append('?').append(normalizePercentEncoding(normalized.getRawQuery()));
        }
        return canonical.toString();
    }

    private String safeEndpointDescription(String value) {
        try {
            URI uri = URI.create(StringUtils.trimToEmpty(value));
            return "scheme=" + StringUtils.defaultString(uri.getScheme(), "<none>")
                    + ", host=" + StringUtils.defaultString(uri.getHost(), "<none>")
                    + ", port=" + uri.getPort();
        } catch (IllegalArgumentException ignored) {
            return "unparseable";
        }
    }

    private String normalizeRawPath(String rawPath) {
        String decodedUnreserved = normalizePercentEncoding(
                StringUtils.defaultIfBlank(rawPath, "/"));
        String normalized = URI.create(decodedUnreserved).normalize().toString();
        return StringUtils.defaultIfBlank(normalized, "/");
    }

    private String normalizePercentEncoding(String rawValue) {
        StringBuilder normalized = new StringBuilder(rawValue.length());
        for (int index = 0; index < rawValue.length(); index++) {
            char character = rawValue.charAt(index);
            if (character != '%' || index + 2 >= rawValue.length()) {
                normalized.append(character);
                continue;
            }
            int high = Character.digit(rawValue.charAt(index + 1), 16);
            int low = Character.digit(rawValue.charAt(index + 2), 16);
            if (high < 0 || low < 0) {
                normalized.append(character);
                continue;
            }
            char decoded = (char) ((high << 4) + low);
            if (isUnreserved(decoded)) {
                normalized.append(decoded);
            } else {
                normalized.append('%')
                        .append(Character.toUpperCase(rawValue.charAt(index + 1)))
                        .append(Character.toUpperCase(rawValue.charAt(index + 2)));
            }
            index += 2;
        }
        return normalized.toString();
    }

    private boolean isUnreserved(char character) {
        return (character >= 'a' && character <= 'z')
                || (character >= 'A' && character <= 'Z')
                || (character >= '0' && character <= '9')
                || character == '-'
                || character == '.'
                || character == '_'
                || character == '~';
    }

    private boolean isBingHost(String host) {
        return "bing.com".equals(host) || host.endsWith(".bing.com");
    }

    private String normalizeHost(String host) {
        if (host == null) {
            return null;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isLocalOrIpHost(String host) {
        if (host == null || host.isBlank()
                || "localhost".equals(host)
                || host.endsWith(".localhost")
                || host.endsWith(".local")
                || host.endsWith(".internal")
                || !host.contains(".")) {
            return true;
        }
        // Redirects to literal IP addresses are never allowed, including public IPs.
        return looksLikeIpLiteral(host);
    }

    private boolean looksLikeIpLiteral(String host) {
        if (host.indexOf(':') >= 0) {
            return true;
        }
        for (int index = 0; index < host.length(); index++) {
            char character = host.charAt(index);
            if (!(character == '.' || Character.isDigit(character))) {
                return false;
            }
        }
        return true;
    }

    private boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        Throwable current = failure;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private List<WebPage> parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setExpandEntityReferences(false);

        Document document = factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(StringUtils.defaultString(xml).getBytes(StandardCharsets.UTF_8)));
        NodeList items = document.getElementsByTagName("item");
        List<WebPage> results = new ArrayList<>();
        for (int index = 0; index < items.getLength() && results.size() < maxResults; index++) {
            Node item = items.item(index);
            if (!(item instanceof Element element)) {
                continue;
            }
            WebPage page = new WebPage();
            page.setName(childText(element, "title"));
            page.setUrl(childText(element, "link"));
            page.setSnippet(childText(element, "description"));
            if (isAbsoluteHttpUrl(page.getUrl())) {
                results.add(page);
            }
        }
        return results;
    }

    private String childText(Element parent, String name) {
        NodeList nodes = parent.getElementsByTagName(name);
        return nodes.getLength() == 0 ? "" : StringUtils.trimToEmpty(nodes.item(0).getTextContent());
    }

    private boolean isAbsoluteHttpUrl(String value) {
        try {
            URI uri = URI.create(StringUtils.trimToEmpty(value));
            return uri.isAbsolute()
                    && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
