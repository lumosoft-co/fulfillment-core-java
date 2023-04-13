package com.agoramp.data;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class FulfillmentDestinationConfig {
    private final String secret;
    private Integer port;
}
