package com.agoramp.data.models.fulfillments;

import com.agoramp.data.models.common.OnlineStatus;
import com.agoramp.data.models.common.TimeSpecification;
import com.agoramp.data.models.common.TimedValue;

public class Command {
    private String command;

    private TimeSpecification delay;
    private TimedValue repeat;
    private OnlineStatus requiredStatus;
    private int requiredSlots;
}
