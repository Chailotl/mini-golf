package com.chai.miniGolf.managers;

import com.chai.miniGolf.events.CourseCompletedEvent;
import com.chai.miniGolf.events.CoursePlayRequestedEvent;
import com.chai.miniGolf.events.HoleCompletedEvent;
import com.chai.miniGolf.events.NextHoleRequestedEvent;
import com.chai.miniGolf.models.Course;
import lombok.Builder;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.chai.miniGolf.Main.getPlugin;
import static com.chai.miniGolf.utils.SharedMethods.isBottomSlab;

public class GolfingCourseManager implements Listener {
    private final Map<UUID, GolfingInfo> golfers = new HashMap<>();
    private BukkitTask golfballPhysicsTask; // TODO: cancel this when someone quits and the golfers map is empty again

    @EventHandler
    private void startPlayingCourse(CoursePlayRequestedEvent event) {
        if (golfers.isEmpty()) {
            startGolfBallPhysicsTask();
        }
        golfers.put(event.golfer().getUniqueId(),
            GolfingInfo.builder()
                .course(event.course())
                .golfball(event.course().playerStartedCourse(event.golfer()))
                .build()
            );
        // TODO: Give player clubs
    }

    @EventHandler
    private void holeCompleted(HoleCompletedEvent event) {
        event.course().playerCompletedHole(event.golfer(), event.score());
    }

    @EventHandler
    private void nextHoleRequested(NextHoleRequestedEvent event) {
        golfers.get(event.golfer().getUniqueId())
            .setGolfball(event.course().playerMovingToNextHole(event.golfer()));
    }

    @EventHandler
    private void courseCompleted(CourseCompletedEvent event) {
        golfers.remove(event.golfer().getUniqueId());
        if (golfers.isEmpty()) {
            golfballPhysicsTask.cancel();
        }
    }

    public GolfingInfo getGolfingInfo(Player p) {
        return golfers.get(p.getUniqueId());
    }

    public Optional<GolfingInfo> getGolfingInfoFromGolfball(Snowball ball) {
        return golfers.values().stream()
            .filter(gi -> ball.equals(gi.getGolfball()))
            .findFirst();
    }

    private void startGolfBallPhysicsTask() {
        golfballPhysicsTask = new BukkitRunnable() {
            @Override
            public void run()
            {
                golfers.entrySet()
                    .stream()
                    .filter(e -> e.getValue().getGolfball() != null)
                    .forEach(e -> {
                        if (!handleGolfBallPhysicsAndDecideIfWeShouldKeepBall(e)) {
                            e.getValue().getGolfball().remove();
                            e.getValue().setGolfball(null);
                        }
                    });
            }
        }.runTaskTimer(getPlugin(), 0L, 1L);
    }

    /* False if we should set the golfball to null. */
    private boolean handleGolfBallPhysicsAndDecideIfWeShouldKeepBall(Map.Entry<UUID, GolfingInfo> golfer) {
        Snowball ball = golfer.getValue().getGolfball();
        if (!ball.isValid()) {
            return false;
        }
        if (ball.getTicksLived() > 1200) {
            ball.getWorld().dropItem(ball.getLocation(), getPlugin().golfBallItemStack());
            ball.remove();
            return false;
        }
        Location loc = ball.getLocation();
        Block block = loc.subtract(0, 0.1, 0).getBlock();
        Vector vel = ball.getVelocity();
        switch (block.getType()) {
            case CAULDRON:
                if (handleCauldronAndDecideIfSuccessfullyCompletedHole(ball, vel, golfer.getKey())) {
                    Bukkit.getPluginManager().callEvent(
                        new HoleCompletedEvent(
                            Bukkit.getPlayer(golfer.getKey()),
                            golfer.getValue().getCourse(),
                            ball.getPersistentDataContainer().get(getPlugin().strokesKey, PersistentDataType.INTEGER)
                        )
                    );
                    return false; // Kill the ball!
                } else {
                    return true; // Keep the ball if it didn't go in the hole cuz it was too fast
                }
            case SOUL_SAND:
                ball.setVelocity(new Vector(0, ball.getVelocity().getY(), 0));
                break;
            case AIR:
                ball.setGravity(true);
                break;
            case SLIME_BLOCK:
                vel.setY(0.25);
                ball.setVelocity(vel);
                break;
            case ICE:
            case PACKED_ICE:
            case BLUE_ICE:
                // No friction
                break;
            case SAND:
            case RED_SAND:
                // Friction
                vel.multiply(0.9);
                ball.setVelocity(vel);
                break;
            case MAGENTA_GLAZED_TERRACOTTA:
                handleGlazedTerracotta(ball, block, vel);
                break;
            default:
                // Check if floating above slabs
                if (isBottomSlab(block) && loc.getY() > block.getY() + 0.5)
                {
                    ball.setGravity(true);
                }

                // Slight friction
                vel.multiply(0.975);
                ball.setVelocity(vel);
                break;
        }
        return true;
    }

    private boolean handleCauldronAndDecideIfSuccessfullyCompletedHole(Snowball ball, Vector vel, UUID golfer) {
        // Check if golf ball is too fast
        if (vel.getY() >= 0 && vel.length() > 0.34)
        {
            return false;
        }

        // Spawn firework
        Firework firework = (Firework) ball.getWorld().spawnEntity(ball.getLocation(), EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(1);
        meta.addEffect(FireworkEffect.builder().withColor(Color.WHITE).with(FireworkEffect.Type.BALL).build());
        firework.setFireworkMeta(meta);
        Bukkit.getScheduler().runTaskLater(getPlugin(), firework::detonate, 20);

        // Send message
        int par = ball.getPersistentDataContainer().get(getPlugin().strokesKey, PersistentDataType.INTEGER);
        Player p = Bukkit.getPlayer(golfer);
        String msg = getPlugin().config().scoreMsg(p.getName(), Integer.toString(par));
        p.sendMessage(msg);
        return true;
    }

    private void handleGlazedTerracotta(Snowball ball, Block block, Vector vel) {
        Directional directional = (Directional) block.getBlockData();
        Vector newVel;
        switch (directional.getFacing()) {
            case NORTH:
                newVel = new Vector(0, 0, 0.1);
                break;
            case SOUTH:
                newVel = new Vector(0, 0, -0.1);
                break;
            case EAST:
                newVel = new Vector(-0.1, 0, 0);
                break;
            case WEST:
                newVel = new Vector(0.1, 0, 0);
                break;
            default:
                return;
        }
        ball.setVelocity(vel.multiply(9.0).add(newVel).multiply(0.1));
    }

    @Data
    @Builder
    public static class GolfingInfo {
        private final Course course;
        private Snowball golfball;
    }
}
