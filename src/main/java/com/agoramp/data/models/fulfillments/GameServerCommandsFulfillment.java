package com.agoramp.data.models.fulfillments;

import com.agoramp.data.models.common.OnlineStatus;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
public class GameServerCommandsFulfillment {
    private List<Command> commands;
    private String target;

    @Getter
    public static class Command {
        private String command;
        private OnlineStatus requiredStatus;
        private Integer requiredSlots;
    }
}
