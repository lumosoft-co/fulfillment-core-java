package com.agoramp;

import com.agoramp.data.models.fulfillments.Fulfillment;
import com.agoramp.data.models.fulfillments.FulfillmentType;
import com.agoramp.data.models.fulfillments.GameServerCommandsFulfillment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface FulfillmentExecutor {

    /**
     * Implement if you expect to receive Game Command Fulfillments.
     * This data helps to narrow down events from the API to fulfillments which can be processed
     * @return A flux of online players
     */
    default Flux<String> retrieveOnlinePlayerIds() {
        return null;
    }

    default <T> Mono<Boolean> processFulfillment(Fulfillment<T> fulfillment) {
        FulfillmentType type = fulfillment.getType();
        switch (type) {
            case COMMAND: return processCommandFulfillment((GameServerCommandsFulfillment) fulfillment.getData());
        }

        return null;
    }

    default Mono<Boolean> processCommandFulfillment(GameServerCommandsFulfillment fulfillment) {
        return null;
    }

}
