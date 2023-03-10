package com.agoramp.data.models.requests;

import com.agoramp.data.models.fulfillments.FulfillmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FulfillmentUpdateRequest {
    private String id;
    private FulfillmentStatus from, to;

}
