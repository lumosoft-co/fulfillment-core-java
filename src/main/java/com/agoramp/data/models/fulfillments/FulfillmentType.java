package com.agoramp.data.models.fulfillments;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum FulfillmentType {
    COMMAND(GameServerCommandsFulfillment.class);

    private final Class<?> type;
}
