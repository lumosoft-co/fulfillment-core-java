package com.agoramp.data;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class FulfillmentDestinationConfig {
    private String secret;
    private Integer port;
}
