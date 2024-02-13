package com.chai.miniGolf.models;

import com.chai.miniGolf.events.CourseCompletedEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.chai.miniGolf.Main.getPlugin;
import static com.chai.miniGolf.Main.logger;

@Jacksonized
@Builder
@Data
public class Course {
    private String name;
    private List<Hole> holes;
    @JsonIgnore
    private Map<UUID, Integer> playersAndTheirCurrentHole;

    private Course(String name, List<Hole> holes, Map<UUID, Integer> playersAndTheirCurrentHole) {
        this.name = name;
        this.holes = holes;
        this.playersAndTheirCurrentHole = new HashMap<>();
    }

    public static Course newCourse(String name) {
        return new Course(name, new ArrayList<>(), null);
    }

    public void addHole(Hole hole, Integer index) {
        this.holes.add(index, hole);
    }

    public Snowball playerStartedCourse(Player p) {
        playersAndTheirCurrentHole.put(p.getUniqueId(), -1);
        return playerMovingToNextHole(p);
    }

    public void playerCompletedHole(Player p, int score) {
        holes.get(playersAndTheirCurrentHole.get(p.getUniqueId())).playerFinishedHole(p, score);
    }

    // Two separate things completing the hole and moving to the next hole so the player can watch the fireworks
    public Snowball playerMovingToNextHole(Player p) {
        int nextHoleIndex = playersAndTheirCurrentHole.get(p.getUniqueId()) + 1;
        if (nextHoleIndex >= holes.size()) {
            // Course Completed
            playerCompletedCourse(p);
            return null;
        }
        holes.get(nextHoleIndex).playerStartedPlayingHole(p);
        playersAndTheirCurrentHole.put(p.getUniqueId(), nextHoleIndex);
        teleportPlayerToHoleStart(p, nextHoleIndex);
        return placeBallForPlayer(p, nextHoleIndex);
    }

    public void teleportPlayerToHoleStart(Player p, int holeIndex) {
        if (holeIndex >= holes.size()) {
            logger().severe(String.format("Player %s has attempted to teleport to holeIndex %s but only %s holes exist for course %s", p.getName(), holeIndex, holes.size(), name));
            return;
        }
        p.teleport(holes.get(holeIndex).getStartingLocation());
    }

    public Snowball placeBallForPlayer(Player p, int holeIndex) {
        Location loc = holes.get(holeIndex).getStartingLocation();
        Snowball ball = (Snowball) p.getWorld().spawnEntity(loc, EntityType.SNOWBALL);
        ball.setGravity(false);
        PersistentDataContainer c = ball.getPersistentDataContainer();
        c.set(getPlugin().xKey, PersistentDataType.DOUBLE, loc.getX());
        c.set(getPlugin().yKey, PersistentDataType.DOUBLE, loc.getY());
        c.set(getPlugin().zKey, PersistentDataType.DOUBLE, loc.getZ());
        c.set(getPlugin().strokesKey, PersistentDataType.INTEGER, 0);
        ball.setCustomName("Par 0");
        ball.setCustomNameVisible(true);
        return ball;
    }

    public int playerTotalScore(Player p) {
        return holes.stream()
            .filter(h -> h.hasPlayerFinishedHole(p))
            .map(h -> h.playersScore(p))
            .reduce(0, Integer::sum);
    }

    public void playerCompletedCourse(Player p) {
        System.out.printf("%s just finished %s with a score of %s%n", p.getName(), name, playerTotalScore(p));
        Bukkit.getPluginManager().callEvent(new CourseCompletedEvent(p, this, playerTotalScore(p)));
        playersAndTheirCurrentHole.remove(p.getUniqueId());
    }
}
