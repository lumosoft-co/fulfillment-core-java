package com.agoramp.data.models.common;

import lombok.Data;

@Data
public class TimedValue {
    private int cycles;
    private TimeSpecification interval;
}
