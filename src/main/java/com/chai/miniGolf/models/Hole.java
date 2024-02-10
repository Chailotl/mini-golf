package com.chai.miniGolf.models;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.bukkit.Location;

@Jacksonized
@Builder
@Data
public class Hole {
    private Integer par;
    private Double startingLocX;
    private Double startingLocY;
    private Double startingLocZ;
    private Float startingLocYaw;
    private Float startingLocPitch;
    private Double holeLocX;
    private Double holeLocY;
    private Double holeLocZ;

    public static Hole newHole(Integer par, Location startingLoc, Location hole) {
        return Hole.builder()
            .par(par)
            .startingLocX(startingLoc.getX())
            .startingLocY(startingLoc.getY())
            .startingLocZ(startingLoc.getZ())
            .startingLocYaw(startingLoc.getYaw())
            .startingLocPitch(startingLoc.getPitch())
            .holeLocX(hole.getX())
            .holeLocY(hole.getY())
            .holeLocZ(hole.getZ())
            .build();
    }
}
