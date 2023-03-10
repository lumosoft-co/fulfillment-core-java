package com.agoramp.data.models.fulfillments;

import lombok.Data;

import java.util.List;

@Data
public class GameServerCommandsFulfillment {
    private List<Command> commands;
    private String target;
}
