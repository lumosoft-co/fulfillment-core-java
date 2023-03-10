package com.agoramp.controller;

import com.agoramp.FulfillmentReceiver;
import reactor.core.publisher.Flux;

import java.time.Duration;

public enum PollingController implements FulfillmentReceiver {
    INSTANCE;

    public void initialize() {
        Flux.interval(Duration.ofMinutes(2))
                .flatMap(i -> processFulfillments())
                .filter(l -> l > 0)
                .subscribe(l -> System.out.printf("Processed %d fulfillments\n", l));
    }
}
