package com.agoramp.controller;

import com.agoramp.FulfillmentReceiver;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public enum PollingController implements FulfillmentReceiver {
    INSTANCE;

    private Disposable subscription;

    public void initialize() {
        subscription = Mono
                .delay(Duration.ofMinutes(2))
                .flatMap(i -> processFulfillments().publishOn(Schedulers.single()))
                .doOnNext(l -> System.out.printf("Processed %d fulfillments\n", l))
                .thenReturn("")
                .defaultIfEmpty("")
                .publishOn(Schedulers.single())
                .retry()
                .repeat()
                .subscribe();
    }

    public void shutdown() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }
}
