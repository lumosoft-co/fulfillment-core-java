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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface FulfillmentReceiver {
    int PAGE_SIZE = 100;

    default Mono<Long> processFulfillments() {
        return onReceive(null);
    }

    default Mono<Long> onReceive(String orderId) {
        Flux<String> onlinePlayersFlux = AgoraFulfillmentService.INSTANCE.getExecutor().retrieveOnlinePlayerIds();
        Mono<Optional<List<String>>> onlinePlayers = onlinePlayersFlux == null ?
                Mono.just(Optional.empty()) :
                onlinePlayersFlux.collectList().map(Optional::of);
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
                        online.orElse(null),
                        0
                ))
                .flatMap(this::processFulfillments);
    }

    default Flux<Fulfillment> pollFulfillments(FulfillmentPollRequest request) {
        return HttpClient.create()
                .baseUrl(Optional.ofNullable(System.getenv("AGORA-API-URL")).orElse("https://api.agoramp.com"))
                .headers(h -> h.add("Content-Type", "application/json"))
                .post()
                .uri("/fulfillments/" + AgoraFulfillmentService.INSTANCE.getConfig().getSecret())
                .send(ByteBufFlux.fromString(Mono.just(new Gson().toJson(request))))
                .response((r, b) -> b.aggregate().asString(StandardCharsets.UTF_8))
                .singleOrEmpty()
                .map(s -> new JsonParser().parse(s))
                .flatMapIterable(JsonElement::getAsJsonArray)
                .map(json -> DataUtil.fromJson(json.toString(), Fulfillment.class))
                .cast(Fulfillment.class);
    }

    default Mono<Long> processFulfillments(FulfillmentPollRequest request) {
        return pollFulfillments(request)
                .flatMap(f -> {
                    Mono<Boolean> handler = AgoraFulfillmentService.INSTANCE.getExecutor().processFulfillment(f);
                    if (handler == null) return Mono.empty();
                    return HttpClient.create()
                            .baseUrl(Optional.ofNullable(System.getenv("AGORA-API-URL")).orElse("https://api.agoramp.com"))
                            .headers(h -> h.add("Content-Type", "application/json"))
                            .post()
                            .uri("/fulfillmentupdate")
                            .send(ByteBufFlux.fromString(Mono.just(new Gson().toJson(new FulfillmentUpdateRequest(
                                    f.getId(),
                                    FulfillmentStatus.PENDING,
                                    FulfillmentStatus.PROCESSING
                            )))))
                            .response()
                            .filter(r -> r.status().code() == 200)
                            .flatMap(r -> handler)
                            .onErrorReturn(false)
                            .defaultIfEmpty(false)
                            .flatMap(success -> HttpClient.create()
                                    .baseUrl(Optional.ofNullable(System.getenv("AGORA-API-URL")).orElse("https://api.agoramp.com"))
                                            .headers(h -> h.add("Content-Type", "application/json"))
                                    .post()
                                    .uri("/fulfillmentupdate")
                                    .send(ByteBufFlux.fromString(Mono.just(new Gson().toJson(new FulfillmentUpdateRequest(
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
                                    .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(10)))
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
