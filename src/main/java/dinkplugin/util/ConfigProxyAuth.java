package dinkplugin.util;

import dinkplugin.DinkPluginConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
@RequiredArgsConstructor
public final class ConfigProxyAuth implements Authenticator {

    private final DinkPluginConfig config;

    @Override
    @Nullable
    public Request authenticate(@Nullable Route route, @NotNull Response response) {
        var server = config.proxyServer();
        if (server == null || server.isBlank()) {
            return null;
        }

        var auth = config.proxyAuth();
        if (auth == null || auth.isBlank()) {
            return null;
        }

        int delim = auth.indexOf(':');
        if (delim < 0) {
            log.warn("Proxy auth must be specified in user:pass format");
            return null;
        }

        var username = auth.substring(0, delim);
        var password = auth.substring(delim + 1);

        return response.request()
            .newBuilder()
            .header("Proxy-Authorization", Credentials.basic(username, password))
            .build();
    }

}
