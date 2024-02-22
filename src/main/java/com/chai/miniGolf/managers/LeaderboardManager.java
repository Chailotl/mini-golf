package com.chai.miniGolf.managers;

import com.chai.miniGolf.events.CourseCompletedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import static com.chai.miniGolf.Main.getPlugin;

public class LeaderboardManager implements Listener {
    @EventHandler
    private void onCourseCompleted(CourseCompletedEvent event) {
        getPlugin().config().newCourseScoreRecorded(event.course(), event.golfer(), event.totalScore());
    }
}
