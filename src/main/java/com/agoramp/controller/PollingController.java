package com.agoramp.controller;

import com.agoramp.FulfillmentReceiver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.retry.Repeat;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public enum PollingController implements FulfillmentReceiver {
    INSTANCE;

    public void initialize() {
        AtomicInteger counter = new AtomicInteger(1);
        Mono.delay(Duration.ofSeconds(10))
                .map(l -> counter.getAndIncrement())
                //.doOnNext(l -> System.out.println("Polling for fulfillments... " + l))
                .flatMap(i -> processFulfillments())
                .filter(l -> l > 0)
                .doOnNext(l -> System.out.printf("Processed %d fulfillments\n", l))
                .retry()
                .repeat()
                .publishOn(Schedulers.boundedElastic())
                .subscribe();
    }
}
