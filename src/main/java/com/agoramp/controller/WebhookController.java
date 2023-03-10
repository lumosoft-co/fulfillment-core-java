package com.agoramp.controller;

import com.agoramp.FulfillmentReceiver;
import com.agoramp.util.AgoraWebhookStatus;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.tcp.TcpServer;

import java.nio.charset.StandardCharsets;

public enum WebhookController implements FulfillmentReceiver {
    INSTANCE;

    private DisposableServer server;

    public void initialize(int port) {
        if (server != null) server.disposeNow();
        server = TcpServer.create()
                .host("0.0.0.0")
                .port(port)
                .handle(this::handle)
                .bindNow();
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
