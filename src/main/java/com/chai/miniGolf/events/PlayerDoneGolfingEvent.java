package com.chai.miniGolf.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerDoneGolfingEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player golfer;

    public PlayerDoneGolfingEvent(Player golfer) {
        this.golfer = golfer;
    }

    public Player golfer() {
        return golfer;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
