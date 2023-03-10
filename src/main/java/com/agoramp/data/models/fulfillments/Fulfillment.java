package com.agoramp.data.models.fulfillments;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Fulfillment<T> {
    private String id;
    private FulfillmentType type;
    private T data;
}
