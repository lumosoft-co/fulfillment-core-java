package com.agoramp;

import com.agoramp.data.models.fulfillments.Fulfillment;
import com.agoramp.data.models.fulfillments.FulfillmentStatus;
import com.agoramp.data.models.fulfillments.FulfillmentType;
import com.agoramp.data.models.requests.FulfillmentPollRequest;
import com.agoramp.data.models.requests.FulfillmentUpdateRequest;
import com.agoramp.util.DataUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface FulfillmentReceiver {
    int PAGE_SIZE = 100;

    default Mono<Long> processFulfillments() {
        return onReceive(null);
    }

    default Mono<Long> onReceive(String orderId) {
        Flux<String> onlinePlayersFlux = AgoraFulfillmentService.INSTANCE.getExecutor().retrieveOnlinePlayerIds();
        Mono<List<String>> onlinePlayers = onlinePlayersFlux == null ?
                Mono.just(Collections.emptyList()) :
                onlinePlayersFlux.collectList();
        List<FulfillmentType> types = new ArrayList<>();
        for (FulfillmentType type : FulfillmentType.values()) {
            Mono<Boolean> handler = AgoraFulfillmentService.INSTANCE.getExecutor().processFulfillment(new Fulfillment<>(
                    "",
                    type,
                    null
            ));
            if (handler != null) {
                types.add(type);
            }
        }
        return onlinePlayers
                .map(online -> new FulfillmentPollRequest(
                        orderId,
                        types,
                        online,
                        0
                ))
                .flatMap(this::processFulfillments);
    }

    default Flux<Fulfillment<?>> pollFulfillments(FulfillmentPollRequest request) {
        return HttpClient.create()
                .baseUrl(Optional.ofNullable(System.getenv("AGORA-API-URL")).orElse("https://admin.agoramp.com"))
                .post()
                .uri("/fulfillments/" + AgoraFulfillmentService.INSTANCE.getConfig().getSecret())
                .send(Mono.just(Unpooled.copiedBuffer(new Gson().toJson(request).getBytes(StandardCharsets.UTF_8))))
                .responseContent()
                .aggregate()
                .map(buf -> new String(buf.array(), StandardCharsets.UTF_8))
                .map(JsonParser::parseString)
                .flatMapIterable(JsonElement::getAsJsonArray)
                .map(json -> (Fulfillment<?>) DataUtil.fromJson(json.toString(), Fulfillment.class));
    }

    default Mono<Long> processFulfillments(FulfillmentPollRequest request) {
        return pollFulfillments(request)
                .flatMap(f -> {
                    Mono<Boolean> handler = AgoraFulfillmentService.INSTANCE.getExecutor().processFulfillment(f);
                    if (handler == null) return Mono.empty();
                    return HttpClient.create()
                            .baseUrl(Optional.ofNullable(System.getenv("AGORA-API-URL")).orElse("https://admin.agoramp.com"))
                            .post()
                            .uri("/fulfillmentupdate")
                            .send(Mono.just(Unpooled.copiedBuffer(DataUtil.toJson(new FulfillmentUpdateRequest(
                                    f.getId(),
                                    FulfillmentStatus.PENDING,
                                    FulfillmentStatus.PROCESSING
                            )))))
                            .response()
                            .filter(r -> r.status().code() == 200)
                            .then(handler)
                            .flatMap(success -> HttpClient.create()
                                    .baseUrl(Optional.ofNullable(System.getenv("AGORA-API-URL")).orElse("https://admin.agoramp.com"))
                                    .post()
                                    .uri("/fulfillmentupdate")
                                    .send(Mono.just(Unpooled.copiedBuffer(DataUtil.toJson(new FulfillmentUpdateRequest(
                                            f.getId(),
                                            null,
                                            success ? FulfillmentStatus.COMPLETED : FulfillmentStatus.PENDING
                                    )))))
                                    .response()
                                    .doOnNext(response -> {
                                        if (response.status().code() != 200) {
                                            throw new Error("server down");
                                        }
                                    })
                                    .retry()
                            );
                })
                .count()
                .flatMap(c -> {
                    if (c < PAGE_SIZE) return Mono.just(c);
                    else return processFulfillments(new FulfillmentPollRequest(
                            request.getOrder(),
                            request.getTypes(),
                            request.getOnlinePlayers(),
                            request.getPage() + 1
                    )).map(l -> l + c);
                });
    }
}
