package com.agoramp.data.models.requests;

import com.agoramp.data.models.fulfillments.FulfillmentType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collection;

@Data
@AllArgsConstructor
public class FulfillmentPollRequest {
    private String order;
    private Collection<FulfillmentType> types;
    private Collection<String> onlinePlayers;
    private int page;
}
