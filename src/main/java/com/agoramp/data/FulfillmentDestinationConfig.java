package com.agoramp.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FulfillmentDestinationConfig {
    private final String secret;
    private Integer port;
}
