package com.chai.miniGolf.models;

import com.chai.miniGolf.events.CourseCompletedEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private String worldUuid;
    private Double endingLocX;
    private Double endingLocY;
    private Double endingLocZ;
    private Float endingLocYaw;
    private Float endingLocPitch;
    private Map<String, Score> leaderboards;
    @JsonIgnore
    private Map<UUID, Integer> playersAndTheirCurrentHole;
    private List<Hole> holes;

    public static Course newCourse(String name, Location currentLoc) {
        return Course.builder()
            .name(name)
            .holes(new ArrayList<>())
            .leaderboards(new LinkedHashMap<>())
            .playersAndTheirCurrentHole(null)
            .worldUuid(currentLoc.getWorld().getUID().toString())
            .endingLocX(currentLoc.getX())
            .endingLocY(currentLoc.getY())
            .endingLocZ(currentLoc.getZ())
            .endingLocYaw(currentLoc.getYaw())
            .endingLocPitch(currentLoc.getPitch())
            .build();
    }

    public void addHole(Hole hole, Integer index) {
        this.holes.add(index, hole);
    }

    public Snowball playerStartedCourse(Player p) {
        playersAndTheirCurrentHole().put(p.getUniqueId(), -1);
        return playerMovingToNextHole(p);
    }

    @JsonIgnore
    public Location getEndingLocation() {
        return new Location(Bukkit.getWorld(UUID.fromString(worldUuid)), endingLocX, endingLocY, endingLocZ, endingLocYaw, endingLocPitch);
    }

    @JsonIgnore
    public Block getCurrentHoleCauldronBlock(UUID pUuid) {
        return holes.get(playersAndTheirCurrentHole().get(pUuid)).getHoleBlock();
    }

    public int playersCurrentHole(UUID pUuid) {
        return playersAndTheirCurrentHole().get(pUuid);
    }

    public void playerCompletedHole(Player p, int score) {
        holes.get(playersAndTheirCurrentHole().get(p.getUniqueId())).playerFinishedHole(p, score);
    }

    // Two separate things completing the hole and moving to the next hole so the player can watch the fireworks
    public Snowball playerMovingToNextHole(Player p) {
        int nextHoleIndex = playersAndTheirCurrentHole().get(p.getUniqueId()) + 1;
        if (nextHoleIndex >= holes.size()) {
            // Course Completed
            playerCompletedCourse(p);
            return null;
        }
        holes.get(nextHoleIndex).playerStartedPlayingHole(p);
        playersAndTheirCurrentHole().put(p.getUniqueId(), nextHoleIndex);
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
        Location loc = holes.get(holeIndex).getBallStartingLocation();
        Snowball ball = (Snowball) p.getWorld().spawnEntity(loc, EntityType.SNOWBALL);
        ball.setGravity(false);
        PersistentDataContainer c = ball.getPersistentDataContainer();
        c.set(getPlugin().xKey, PersistentDataType.DOUBLE, loc.getX());
        c.set(getPlugin().yKey, PersistentDataType.DOUBLE, loc.getY());
        c.set(getPlugin().zKey, PersistentDataType.DOUBLE, loc.getZ());
        c.set(getPlugin().strokesKey, PersistentDataType.INTEGER, 0);
        ball.setCustomName("Stroke 0");
        ball.setCustomNameVisible(true);
        return ball;
    }

    public int playerTotalScore(Player p) {
        return holes.stream()
            .filter(h -> h.hasPlayerFinishedHole(p))
            .map(h -> h.playersScore(p))
            .reduce(0, Integer::sum);
    }

    public int totalPar() {
        return holes.stream()
            .map(Hole::getPar)
            .reduce(0, Integer::sum);
    }

    public void playerCompletedCourse(Player p) {
        Bukkit.getPluginManager().callEvent(new CourseCompletedEvent(p, this, playerTotalScore(p)));
        playersAndTheirCurrentHole().remove(p.getUniqueId());
    }

    private Map<UUID, Integer> playersAndTheirCurrentHole() {
        if (playersAndTheirCurrentHole == null) {
            playersAndTheirCurrentHole = new HashMap<>();
        }
        return playersAndTheirCurrentHole;
    }

    private Map<String, Score> leaderboards() {
        if (leaderboards == null) {
            leaderboards = new HashMap<>();
        }
        return leaderboards;
    }

    public boolean playerGotNewPb(Player p, int totalScore) {
        Score currentPb = leaderboards().get(p.getName());
        int bestScore;
        if (currentPb == null) {
            bestScore = Integer.MAX_VALUE;
        } else {
            bestScore = currentPb.getScore();
        }
        if (totalScore < bestScore) {
            leaderboards.put(p.getName(), new Score(totalScore, System.currentTimeMillis() / 1000));
            return true;
        }
        return false;
    }

    public void playerQuit(Player p) {
        holes.stream()
            .filter(h -> h.playersScore(p) != null)
            .forEach(h -> h.playerDoneWithCourse(p));
        playersAndTheirCurrentHole().remove(p.getUniqueId());
    }

    public void removeHole(int holeToRemoveIndex) {
        holes.remove(holeToRemoveIndex);
    }
}
