package com.chai.miniGolf.events;

import com.chai.miniGolf.models.Course;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CourseCompletedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player golfer;
    private final Course course;
    private final int totalScore;

    public CourseCompletedEvent(Player golfer, Course course, int totalScore) {
        this.golfer = golfer;
        this.course = course;
        this.totalScore = totalScore;
    }

    public Player golfer() {
        return golfer;
    }

    public Course course() {
        return course;
    }

    public int totalScore() {
        return totalScore;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
