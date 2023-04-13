package com.agoramp.data.models.fulfillments;

import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
public class GameServerCommandsFulfillment {
    private List<Command> commands;
    private String target;

    public static class Command {
        @Getter
        private String command;
    }
}
