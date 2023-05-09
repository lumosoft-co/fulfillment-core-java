package com.agoramp.data.models.fulfillments;

import com.agoramp.data.models.common.OnlineStatus;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
public class GameServerCommandsFulfillment {
    private List<String> commands;
    private String target;
    private OnlineStatus requiredStatus;
    private int requiredSlots;
}
