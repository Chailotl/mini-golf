package com.chai.miniGolf.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Jacksonized
@Builder
@Data
public class Hole {
    private Integer par;
    private String worldUuid;
    private Double startingLocX;
    private Double startingLocY;
    private Double startingLocZ;
    private Float startingLocYaw;
    private Float startingLocPitch;
    private Double ballStartingLocX;
    private Double ballStartingLocY;
    private Double ballStartingLocZ;
    private Double holeLocX;
    private Double holeLocY;
    private Double holeLocZ;
    @JsonIgnore
    private Map<UUID, Integer> playerScores;

    public static Hole newHole(Integer par, Location startingLoc, Location ballStartingLoc, Location hole) {
        return Hole.builder()
            .par(par)
            .worldUuid(startingLoc.getWorld().getUID().toString())
            .startingLocX(startingLoc.getX())
            .startingLocY(startingLoc.getY())
            .startingLocZ(startingLoc.getZ())
            .startingLocYaw(startingLoc.getYaw())
            .startingLocPitch(startingLoc.getPitch())
            .ballStartingLocX(startingLoc.getX())
            .ballStartingLocY(startingLoc.getY())
            .ballStartingLocZ(startingLoc.getZ())
            .holeLocX(hole.getX())
            .holeLocY(hole.getY())
            .holeLocZ(hole.getZ())
            .build();
    }

    @JsonIgnore
    public Location getStartingLocation() {
        return new Location(Bukkit.getWorld(UUID.fromString(worldUuid)), startingLocX, startingLocY, startingLocZ, startingLocYaw, startingLocPitch);
    }

    @JsonIgnore
    public Block getHoleBlock() {
        return new Location(Bukkit.getWorld(UUID.fromString(worldUuid)), holeLocX, holeLocY, holeLocZ).getBlock();
    }

    public void playerStartedPlayingHole(Player p) {
        playerScores().put(p.getUniqueId(), -1);
    }

    public void playerFinishedHole(Player p, int score) {
        playerScores().put(p.getUniqueId(), score);
    }

    public void playerDoneWithCourse(Player p) {
        playerScores().remove(p.getUniqueId());
        System.out.println("Player scores: " + playerScores);
    }

    public boolean hasPlayerFinishedHole(Player p) {
        return playerScores().get(p.getUniqueId()) > -1;
    }

    public Integer playersScore(Player p) {
        return playerScores().get(p.getUniqueId());
    }

    @JsonIgnore
    private Map<UUID, Integer> playerScores() {
        if (playerScores == null) {
            playerScores = new HashMap<>();
        }
        return playerScores;
    }
}
