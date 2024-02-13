package com.chai.miniGolf;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import com.chai.miniGolf.utils.ShortUtils.ShortUtils;

import static com.chai.miniGolf.Main.getPlugin;

public class PuttListener implements Listener {
	@EventHandler
	public void onPutt(PlayerInteractEvent event)
	{
		ItemStack item = event.getItem();
		if (ShortUtils.interacting(event) || item == null || !(event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
			return;
		}
		Player p = event.getPlayer();
		World world = p.getWorld();
		Action act = event.getAction();
		Block block = event.getClickedBlock();
		ItemMeta meta = item.getItemMeta();
		boolean putter = ShortUtils.hasKey(meta, getPlugin().putterKey);
		boolean iron = ShortUtils.hasKey(meta, getPlugin().ironKey);
		boolean wedge = ShortUtils.hasKey(meta, getPlugin().wedgeKey);

		if (putter || iron || wedge) {
			// Cancel original tool
			ShortUtils.cancelOriginalTool(event);

			Location eye = p.getEyeLocation();
			Vector dir = eye.getDirection();
			Vector loc = eye.toVector();

			Snowball ball = getPlugin().golfingCourseManager().getGolfingInfo(p).getGolfball();
			if (!isWithinInteractableRange(ball.getLocation(), p.getEyeLocation(), 5.5)) {
				return;
			}
			// Is golf ball in player's view?
			Vector vec = ball.getLocation().toVector().subtract(loc);
			if (dir.angle(vec) >= 0.15f) {
				return;
			}

			// Hit golf ball
			dir.setY(0).normalize();
			boolean crit = p.getVelocity().getY() < -0.08;
			if (iron) {
				dir.multiply(crit ? 1 : p.isSneaking() ? 0.6666 : 0.8333);
			}
			else if (putter) {
				dir.multiply(crit ? 0.5 : p.isSneaking() ? 0.1666 : 0.3333);
			}
			else if (wedge) {
				dir.multiply(crit ? 0 : p.isSneaking() ? 0.125 : 0.25);
				dir.setY(0.15);
			}

			ball.setVelocity(dir);
			PersistentDataContainer c = ball.getPersistentDataContainer();

			// Update par
			int strokes = c.get(getPlugin().strokesKey, PersistentDataType.INTEGER) + 1;
			c.set(getPlugin().strokesKey, PersistentDataType.INTEGER, strokes);
			ball.setCustomName("Strokes: " + strokes);

			// Update last pos
			Location lastPos = ball.getLocation();
			c.set(getPlugin().xKey, PersistentDataType.DOUBLE, lastPos.getX());
			c.set(getPlugin().yKey, PersistentDataType.DOUBLE, lastPos.getY());
			c.set(getPlugin().zKey, PersistentDataType.DOUBLE, lastPos.getZ());

			// Add to map
			//plugin.golfBalls.add((Snowball) ent); TODO: why was this being added again?
			ball.setTicksLived(1);

			// Effects
			if (crit) {
				world.spawnParticle(Particle.CRIT, ball.getLocation(), 15, 0, 0, 0, 0.25);
			}
			world.playSound(ball.getLocation(), Sound.BLOCK_METAL_HIT, crit ? 1f : p.isSneaking() ? 0.5f : 0.75f, 1.25f);
		}
	}

	private boolean isWithinInteractableRange(Location ballLocation, Location eyeLocation, double range) {
		if (!ballLocation.getWorld().equals(eyeLocation.getWorld())) {
			return false;
		}
		return Math.abs(ballLocation.getX() - eyeLocation.getX()) < range &&
			Math.abs(ballLocation.getY() - eyeLocation.getY()) < range &&
			Math.abs(ballLocation.getZ() - eyeLocation.getZ()) < range;
	}
}