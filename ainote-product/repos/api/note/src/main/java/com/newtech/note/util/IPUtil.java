package com.newtech.note.util;

import org.springframework.http.server.reactive.ServerHttpRequest;
import java.util.Optional;
 
/**
 * 用户获得用户ip的工具类
 */
public class IPUtil {

    /**
     * Returns the transport peer only. Forwarding headers are untrusted unless a separately
     * configured trusted-proxy boundary validates and rewrites them before this application.
     */
    public static String getRequestIp(ServerHttpRequest request) {
        return Optional.ofNullable(request.getRemoteAddress())
                .map(address -> address.getAddress().getHostAddress())
                .orElse("unknown");
    }
 
    /**
     * Backward-compatible alias with the same untrusted-forwarding-header policy.
     */
    public static String getIP(ServerHttpRequest request) {
        return getRequestIp(request);
    }
}
