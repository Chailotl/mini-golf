package com.chai.miniGolf.events;

import com.chai.miniGolf.models.Course;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NextHoleRequestedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player golfer;
    private final Course course;

    public NextHoleRequestedEvent(Player golfer, Course course) {
        this.golfer = golfer;
        this.course = course;
    }

    public Player golfer() {
        return golfer;
    }

    public Course course() {
        return course;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
