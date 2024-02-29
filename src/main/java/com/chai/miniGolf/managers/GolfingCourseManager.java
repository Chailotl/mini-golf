package com.chai.miniGolf.managers;

import com.chai.miniGolf.events.CourseCompletedEvent;
import com.chai.miniGolf.events.CoursePlayRequestedEvent;
import com.chai.miniGolf.events.HoleCompletedEvent;
import com.chai.miniGolf.events.NextHoleRequestedEvent;
import com.chai.miniGolf.events.PlayerDoneGolfingEvent;
import com.chai.miniGolf.models.Course;
import com.chai.miniGolf.utils.ShortUtils.ShortUtils;
import lombok.Builder;
import lombok.Data;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

import static com.chai.miniGolf.Main.getPlugin;
import static com.chai.miniGolf.managers.ScorecardManager.holeResultColor;
import static com.chai.miniGolf.managers.ScorecardManager.holeResultString;
import static com.chai.miniGolf.utils.SharedMethods.isBottomSlab;
import static org.bukkit.persistence.PersistentDataType.DOUBLE;
import static org.bukkit.persistence.PersistentDataType.INTEGER;
import static org.bukkit.persistence.PersistentDataType.STRING;

public class GolfingCourseManager implements Listener {
    private final Map<UUID, GolfingInfo> golfers = new HashMap<>();
    private BukkitTask golfballPhysicsTask;

    @EventHandler
    private void startPlayingCourse(CoursePlayRequestedEvent event) {
        if (golfers.isEmpty()) {
            startGolfBallPhysicsTask();
        }
        GolfingInfo golfersOldShtuff = golfers.get(event.golfer().getUniqueId());
        if (golfersOldShtuff != null && golfersOldShtuff.getGolfball() != null) {
            golfersOldShtuff.getGolfball().remove();
        }
        golfers.put(event.golfer().getUniqueId(),
            GolfingInfo.builder()
                .course(event.course())
                .golfball(event.course().playerStartedCourse(event.golfer()))
                .invBeforeGolfing(event.golfer().getInventory().getContents().clone())
                .hidingOthers(false)
                .build()
            );
        event.golfer().getInventory().clear();
        event.golfer().getInventory().setItem(0, getPlugin().ironItemStack());
        event.golfer().getInventory().setItem(1, getPlugin().wedgeItemStack());
        event.golfer().getInventory().setItem(2, getPlugin().putterItemStack());
        event.golfer().getInventory().setItem(4, getPlugin().whistleItemStack());
        event.golfer().getInventory().setItem(5, getPlugin().nextHoleItemItemStack());
        event.golfer().getInventory().setItem(6, getPlugin().scorecardItemStack());
        event.golfer().getInventory().setItem(7, getPlugin().hideOthersItemItemStack());
        event.golfer().getInventory().setItem(8, getPlugin().quitItemItemStack());
        cleanUpAnyUnusedBalls(event.golfer());
    }

    @EventHandler
    private void holeCompleted(HoleCompletedEvent event) {
        event.course().playerCompletedHole(event.golfer(), event.score());
    }

    @EventHandler
    private void golfingItemUsed(PlayerInteractEvent event) {
        GolfingCourseManager.GolfingInfo golferInfo = getGolfingInfo(event.getPlayer());
        if (golferInfo == null
            || !(event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
            || event.getItem() == null
            || event.getItem().getItemMeta() == null) {
            return;
        }
        if (ShortUtils.hasKey(event.getItem().getItemMeta(), getPlugin().nextHoleItemKey) && event.getPlayer().getCooldown(getPlugin().nextHoleItemItemStack().getType()) == 0) {
            Bukkit.getPluginManager().callEvent(new NextHoleRequestedEvent(event.getPlayer(), golferInfo.getCourse()));
            event.getPlayer().setCooldown(getPlugin().nextHoleItemItemStack().getType(), 10);
            event.setCancelled(true);
        } else if (ShortUtils.hasKey(event.getItem().getItemMeta(), getPlugin().quitItemKey)) {
            Bukkit.getPluginManager().callEvent(new PlayerDoneGolfingEvent(event.getPlayer()));
            event.setCancelled(true);
        } else if (ShortUtils.hasKey(event.getItem().getItemMeta(), getPlugin().whistleKey)) {
            event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.9f, 1.9f);
            returnBallToLastLoc(event.getPlayer().getUniqueId());
            event.setCancelled(true);
        } else if (ShortUtils.hasKey(event.getItem().getItemMeta(), getPlugin().hideOthersItemKey) && event.getPlayer().getCooldown(getPlugin().hideOthersItemItemStack().getType()) == 0) {
            toggleVisibility(event.getPlayer());
            event.getPlayer().setCooldown(getPlugin().nextHoleItemItemStack().getType(), 1);
            event.setCancelled(true);
        }
    }

    private void toggleVisibility(Player p) {
        GolfingInfo golfingInfo = golfers.get(p.getUniqueId());
        if (golfingInfo == null) {
            return;
        }
        golfingInfo.setHidingOthers(!golfingInfo.isHidingOthers());
        BiConsumer<Plugin, Player> toggleVisibilityPlayerMethod = p::showPlayer;
        BiConsumer<Plugin, Entity> toggleVisibilityEntityMethod = p::showEntity;
        if (golfingInfo.isHidingOthers()) {
            toggleVisibilityPlayerMethod = p::hidePlayer;
            toggleVisibilityEntityMethod = p::hideEntity;
        }
        for (Map.Entry<UUID, GolfingInfo> entry : golfers.entrySet()) {
            if (entry.getKey().equals(p.getUniqueId())) {
                continue;
            }
            Player otherGolfer = Bukkit.getPlayer(entry.getKey());
            Snowball otherGolfball = entry.getValue().getGolfball();
            if (otherGolfer != null) {
                toggleVisibilityPlayerMethod.accept(getPlugin(), otherGolfer);
            }
            if (otherGolfball != null) {
                toggleVisibilityEntityMethod.accept(getPlugin(), otherGolfball);
            }
        }
    }

    @EventHandler
    private void onGolfballSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Snowball ball) {
            String owner = ball.getPersistentDataContainer().get(getPlugin().ownerNameKey, STRING);
            for (Map.Entry<UUID, GolfingInfo> entry : golfers.entrySet()) {
                Player otherGolfer = Bukkit.getPlayer(entry.getKey());
                if (otherGolfer != null && !otherGolfer.getName().equals(owner) && entry.getValue().isHidingOthers()) {
                    otherGolfer.hideEntity(getPlugin(), ball);
                }
            }
        }
    }

    @EventHandler
    private void onPlayerBlockBreak(BlockBreakEvent event) {
        if (golfers.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerDropItem(PlayerDropItemEvent event) {
        if (golfers.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onFireworkDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework && golfers.containsKey(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void returnBallToLastLoc(UUID pUuid) {
        GolfingInfo golfingInfo = golfers.get(pUuid);
        if (golfingInfo == null || golfingInfo.getGolfball() == null) {
            if (Bukkit.getPlayer(pUuid) != null) {
                Bukkit.getPlayer(pUuid).sendMessage("Could not find a golfball to return.");
            }
            return;
        }
        PersistentDataContainer c = golfingInfo.getGolfball().getPersistentDataContainer();
        int strokes = c.get(getPlugin().strokesKey, INTEGER) + 1;
        String owner = c.get(getPlugin().ownerNameKey, STRING);
        c.set(getPlugin().strokesKey, INTEGER, strokes);
        c.set(getPlugin().ownerNameKey, STRING, owner);
        golfingInfo.getGolfball().setCustomName(owner + " - " + strokes);
        golfingInfo.getGolfball().setVelocity(new Vector(0, 0, 0));
        golfingInfo.getGolfball().teleport(new Location(golfingInfo.getGolfball().getWorld(), c.get(getPlugin().xKey, DOUBLE), c.get(getPlugin().yKey, DOUBLE), c.get(getPlugin().zKey, DOUBLE)));
        golfingInfo.getGolfball().setGravity(false);
    }

    public void returnBallToLastLoc(Snowball ball) {
        getPUuidFromGolfball(ball).ifPresent(this::returnBallToLastLoc);
    }

    @EventHandler
    private void nextHoleRequested(NextHoleRequestedEvent event) {
        GolfingCourseManager.GolfingInfo golferInfo = getGolfingInfo(event.golfer());
        if (golferInfo == null || !golferInfo.getCourse().equals(event.course())) {
            return;
        }
        Course course = event.course();
        Player p = event.golfer();
        if (course.getHoles().get(course.playersCurrentHole(p.getUniqueId())).hasPlayerFinishedHole(p)) {
            golfers.get(event.golfer().getUniqueId())
                .setGolfball(event.course().playerMovingToNextHole(event.golfer()));
            cleanUpAnyUnusedBalls(event.golfer());
        } else {
            p.sendMessage(String.format("%sYou cannot go to the next hole until you complete this one.%s", ChatColor.GRAY, ChatColor.RESET));
        }
    }

    @EventHandler
    private void courseCompleted(CourseCompletedEvent event) {
        ChatColor scoreColor = event.totalScore() == event.course().totalPar() ? ChatColor.WHITE : event.totalScore() < event.course().totalPar() ? ChatColor.GREEN : ChatColor.RED;
        event.golfer().sendMessage(getPlugin().config().courseCompletedMsg(event.course().getName(), String.valueOf(event.course().totalPar()), String.format("%s%s", scoreColor, event.totalScore())));
        Bukkit.getPluginManager().callEvent(new PlayerDoneGolfingEvent(event.golfer()));
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        Bukkit.getPluginManager().callEvent(new PlayerDoneGolfingEvent(event.getPlayer()));
    }

    @EventHandler
    private void playerDoneWithCurrentCourse(PlayerDoneGolfingEvent event) {
        Player p = event.golfer();
        GolfingInfo golfingInfo = golfers.get(p.getUniqueId());
        if (golfingInfo != null) {
            Snowball ball = golfingInfo.getGolfball();
            if (ball != null) {
                ball.remove();
            }
            golfingInfo.getCourse().playerQuit(p);
            golfers.remove(p.getUniqueId());
            cleanUpAnyUnusedBalls(p);
            p.getInventory().setContents(golfingInfo.getInvBeforeGolfing());
            p.teleport(golfingInfo.getCourse().getEndingLocation());
            if (golfers.isEmpty()) {
                golfballPhysicsTask.cancel();
            }
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

    private Optional<UUID> getPUuidFromGolfball(Snowball ball) {
        return golfers.entrySet().stream()
            .filter(e -> ball.equals(e.getValue().getGolfball()))
            .map(Map.Entry::getKey)
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
        Location loc = ball.getLocation();
        Block block = loc.subtract(0, 0.1, 0).getBlock();
        Vector vel = ball.getVelocity();
        switch (block.getType()) {
            case CAULDRON:
                return !handleCauldronAndDecideIfSuccessfullyCompletedHole(ball, vel, block, golfer); // Kill the ball if it went in a hole
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
                vel.multiply(getPlugin().config().getSandFriction());
                ball.setVelocity(vel);
                break;
            case MAGENTA_GLAZED_TERRACOTTA:
                handleGlazedTerracotta(ball, block, vel);
                break;
            case WATER:
                returnBallToLastLoc(ball);
                break;
            default:
                // Check if floating above slabs
                if (isBottomSlab(block) && loc.getY() > block.getY() + 0.5)
                {
                    ball.setGravity(true);
                }

                // Slight friction
                vel.multiply(getPlugin().config().getFriction());
                if (vel.lengthSquared() < 0.0001) {
                    vel.zero();
                }
                ball.setVelocity(vel);
                break;
        }
        return true;
    }

    private boolean handleCauldronAndDecideIfSuccessfullyCompletedHole(Snowball ball, Vector vel, Block cauldron, Map.Entry<UUID, GolfingInfo> golfer) {
        // Check if golf ball is too fast or is wrong hole cauldron
        if ((vel.getY() >= 0 && vel.length() > 0.34) || !cauldron.equals(golfer.getValue().getCourse().getCurrentHoleCauldronBlock(golfer.getKey()))) {
            return false;
        }

        Course course = golfer.getValue().getCourse();

        // Send message
        int par = ball.getPersistentDataContainer().get(getPlugin().strokesKey, INTEGER);
        Player p = Bukkit.getPlayer(golfer.getKey());
        String msg = getPlugin().config().scoreMsg(p.getName(), String.valueOf(course.playersCurrentHole(golfer.getKey()) + 1), Integer.toString(par));
        p.sendMessage(msg);
        p.showTitle(Title.title(Component.text(holeResultString(course.getHoles().get(course.playersCurrentHole(golfer.getKey())), par)), Component.empty(), Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO)));

        // Spawn firework
        Firework firework = (Firework) ball.getWorld().spawnEntity(ball.getLocation(), EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(1);
        meta.addEffect(FireworkEffect.builder().withColor(holeResultColor(course.getHoles().get(course.playersCurrentHole(golfer.getKey())), par)).with(FireworkEffect.Type.BALL).build());
        firework.setFireworkMeta(meta);
        Bukkit.getScheduler().runTaskLater(getPlugin(), firework::detonate, 20);


        // Let any listeners know that a hole was just completed
        Bukkit.getPluginManager().callEvent(
            new HoleCompletedEvent(
                Bukkit.getPlayer(golfer.getKey()),
                course,
                ball.getPersistentDataContainer().get(getPlugin().strokesKey, INTEGER)
            )
        );
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

    private void cleanUpAnyUnusedBalls(Player golfer) {
        for (Entity entity : golfer.getNearbyEntities(50, 50, 50)) {
            if (entity instanceof Snowball ball && ball.getPersistentDataContainer().has(getPlugin().strokesKey, INTEGER)) {
                Optional<GolfingInfo> maybeGolfingInfo = getGolfingInfoFromGolfball(ball);
                if (maybeGolfingInfo.isEmpty()) {
                    ball.remove();
                }
            }
        }
    }

    @Data
    @Builder
    public static class GolfingInfo {
        private final Course course;
        private Snowball golfball;
        private ItemStack[] invBeforeGolfing;
        private boolean hidingOthers;

        public void setGolfball(Snowball ball) {
            if (golfball != null) {
                golfball.remove();
            }
            golfball = ball;
        }
    }
}
