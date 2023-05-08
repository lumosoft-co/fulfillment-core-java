package com.agoramp.controller;

import com.agoramp.AgoraFulfillmentService;
import com.agoramp.FulfillmentReceiver;
import com.agoramp.util.AgoraWebhookStatus;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpServer;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public enum WebhookController implements FulfillmentReceiver {
    INSTANCE;

    private DisposableServer server;

    public void initialize(int port, String secret) {
        if (server != null) server.disposeNow();
        server = TcpServer.create()
                .host("0.0.0.0")
                .port(port)
                .handle(this::handle)
                .bindNow();

        HttpClient.create()
                .baseUrl(Optional.ofNullable(System.getenv("AGORA-API-URL")).orElse("https://api.agoramp.com"))
                .headers(h -> h.add(HttpHeaderNames.CONTENT_TYPE, "application/json"))
                .post()
                .uri("/bind")
                .send(Mono.just(Unpooled.wrappedBuffer(String.format("{\"destination\":\"%s\", \"port\": %d}", secret, server.port()).getBytes(StandardCharsets.UTF_8))))
                .response()
                .filter(r -> r.status() == HttpResponseStatus.OK)
                .subscribe(r -> System.out.println("Agora webhook support has been bound to port " + server.port()));
    }

    public void shutdown() {
        if (server == null) return;
        server.disposeNow();
        server = null;
    }

    private Publisher<Void> handle(NettyInbound inbound, NettyOutbound outbound) {
        try {
            return outbound.sendByteArray(
                    inbound
                            .receive()
                            .aggregate()
                            .filter(buf -> buf.readableBytes() < 32000) // max length 32kb
                            .map(bytes -> new String(bytes.array(), StandardCharsets.UTF_8))
                            .flatMap(orderId -> {
                                try {
                                    return onReceive(orderId);
                                } catch (Throwable t) {
                                    return Mono.empty();
                                }
                            })
                            .thenReturn(AgoraWebhookStatus.SUCCESS)
                            .defaultIfEmpty(AgoraWebhookStatus.FAILURE)
            );

        } catch (Throwable t) {
            return outbound
                    .sendByteArray(Mono.just(AgoraWebhookStatus.FAILURE))
                    .then();
        }
    }
}
