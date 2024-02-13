package com.chai.miniGolf.events;

import com.chai.miniGolf.models.Course;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class HoleCompletedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player golfer;
    private final Course course;
    private final int score;

    public HoleCompletedEvent(Player golfer, Course course, int score) {
        System.out.println(String.format("HoleCompletedEvent created for %s on course %s", golfer.getName(), course.getName()));
        this.golfer = golfer;
        this.course = course;
        this.score = score;
    }

    public Player golfer() {
        return golfer;
    }

    public Course course() {
        return course;
    }

    public int score() {
        return score;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
