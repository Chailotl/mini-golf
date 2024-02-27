package com.chai.miniGolf.configs;

import lombok.Builder;
import lombok.Data;
import org.bukkit.entity.Player;

@Data
@Builder
public class ClubPower {
    private Double minPowerSneaking;
    private Double minYPowerSneaking;
    private Double maxPowerSneaking;
    private Double maxYPowerSneaking;
    private Double minPowerStanding;
    private Double minYPowerStanding;
    private Double maxPowerStanding;
    private Double maxYPowerStanding;
    private Double minPowerCrit;
    private Double minYPowerCrit;
    private Double maxPowerCrit;
    private Double maxYPowerCrit;

    public Double minPowerForPlayer(Player p) {
        if (p.isSneaking()) {
            return minPowerSneaking;
        } else if (p.getVelocity().getY() < -0.08) {
            return minPowerCrit;
        }
        return minPowerStanding;
    }

    public Double minYPowerForPlayer(Player p) {
        if (p.isSneaking()) {
            return minYPowerSneaking;
        } else if (p.getVelocity().getY() < -0.08) {
            return minYPowerCrit;
        }
        return minYPowerStanding;
    }

    public Double maxPowerForPlayer(Player p) {
        if (p.isSneaking()) {
            return maxPowerSneaking;
        } else if (p.getVelocity().getY() < -0.08) {
            return maxPowerCrit;
        }
        return maxPowerStanding;
    }

    public Double maxYPowerForPlayer(Player p) {
        if (p.isSneaking()) {
            return maxYPowerSneaking;
        } else if (p.getVelocity().getY() < -0.08) {
            return maxYPowerCrit;
        }
        return maxYPowerStanding;
    }
}
