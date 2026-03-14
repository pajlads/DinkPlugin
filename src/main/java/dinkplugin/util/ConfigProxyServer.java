package dinkplugin.util;

import dinkplugin.DinkPluginConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public final class ConfigProxyServer extends ProxySelector {
    private static final List<Proxy> NONE = List.of(Proxy.NO_PROXY);

    private final DinkPluginConfig config;

    @Override
    public List<Proxy> select(URI uri) {
        var server = config.proxyServer();
        if (server == null || server.isBlank()) {
            return NONE;
        }

        int delim = server.lastIndexOf(':');
        if (delim < 0) {
            log.warn("Proxy server must be specified in host:port format");
            return NONE;
        }

        var host = server.substring(0, delim);
        var portStr = server.substring(delim + 1);

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            log.warn("Port for proxy server must be an integer");
            return NONE;
        }

        try {
            var addr = new InetSocketAddress(host, port);
            var proxy = new Proxy(Proxy.Type.HTTP, addr);
            return List.of(proxy);
        } catch (Exception e) {
            log.warn("Proxy server could not be configured", e);
            return NONE;
        }
    }

    @Override
    public void connectFailed(URI uri, SocketAddress socketAddress, IOException e) {
        // no op
    }

}
