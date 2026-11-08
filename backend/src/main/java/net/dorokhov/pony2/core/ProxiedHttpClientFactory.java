package net.dorokhov.pony2.core;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;

import static com.google.common.base.Preconditions.checkArgument;

@Component
public class ProxiedHttpClientFactory {

    private final OkHttpClient httpClient;

    public ProxiedHttpClientFactory(@Value("${pony.proxy.url}") URI proxyUrl) {
        checkArgument("http".equalsIgnoreCase(proxyUrl.getScheme()) && proxyUrl.getHost() != null,
                "Proxy URL must specify an HTTP proxy host.");
        int proxyPort = proxyUrl.getPort() != -1 ? proxyUrl.getPort() : 80;
        httpClient = new OkHttpClient.Builder()
                .proxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxyUrl.getHost(), proxyPort)))
                .build();
    }

    public OkHttpClient.Builder newBuilder() {
        return httpClient.newBuilder();
    }
}
