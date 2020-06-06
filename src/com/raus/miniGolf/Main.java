package com.raus.miniGolf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.raus.craftLib.CraftLib;
import com.raus.shortUtils.ShortUtils;

import net.md_5.bungee.api.ChatColor;

public class Main extends JavaPlugin
{
	private final Plugin craftLib = getServer().getPluginManager().getPlugin("CraftLib");

	// NamespacedKeys
	public final NamespacedKey ballKey = new NamespacedKey(this, "golf_ball");
	public final NamespacedKey putterKey = new NamespacedKey(this, "putter");
	public final NamespacedKey ironKey = new NamespacedKey(this, "iron");
	public final NamespacedKey wedgeKey = new NamespacedKey(this, "wedge");
	public final NamespacedKey whistleKey = new NamespacedKey(this, "return_whistle");

	public final NamespacedKey parKey = new NamespacedKey(this, "par");
	public final NamespacedKey xKey = new NamespacedKey(this, "x");
	public final NamespacedKey yKey = new NamespacedKey(this, "y");
	public final NamespacedKey zKey = new NamespacedKey(this, "z");

	// Stuff
	private final AttributeModifier noDamage = new AttributeModifier(UUID.randomUUID(), "generic.attackDamage", -10, Operation.ADD_NUMBER, EquipmentSlot.HAND);
	private final AttributeModifier fastSwing = new AttributeModifier(UUID.randomUUID(), "generic.attackSpeed", 10, Operation.MULTIPLY_SCALAR_1, EquipmentSlot.HAND);

	private final FireworkEffect fx = FireworkEffect.builder().withColor(Color.WHITE).with(Type.BALL).build();

	// Constants
	public final double floorOffset = 0.05;

	// Lists and maps
	public List<Snowball> golfBalls = new LinkedList<>();
	public Map<UUID, Snowball> lastPlayerBall = new HashMap<>();

	// Items
	private ItemStack golfBall;

	// config stuff
	private String scoreMsg;
	private final String[] tokens = new String[] { "&v1", "&v2" };

	@Override
	public void onEnable()
	{
		// Config
		saveDefaultConfig();
		reload();

		// Register command
		this.getCommand("minigolf").setExecutor(new ReloadCommand());

		// Listeners
		getServer().getPluginManager().registerEvents(new PuttListener(), this);
		getServer().getPluginManager().registerEvents(new ProjectileListener(), this);
		getServer().getPluginManager().registerEvents(new UnloadListener(), this);

		// Iron item
		ItemStack iron = new ItemStack(Material.IRON_HOE);
		ItemMeta meta = iron.getItemMeta();
		meta.setDisplayName("§rIron");
		meta.setLore(Arrays.asList("§8A well-rounded club", "§8for longer distances."));
		meta.setCustomModelData(2);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, noDamage);
		meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, fastSwing);
		ShortUtils.addKey(meta, ironKey);
		iron.setItemMeta(meta);

		ShapedRecipe recipe = new ShapedRecipe(ironKey, iron);
		recipe.shape(" s", "is", "ii");
		recipe.setIngredient('s', Material.STICK);
		recipe.setIngredient('i', Material.IRON_INGOT);
		Bukkit.addRecipe(recipe);

		// Wedge item
		ItemStack wedge = new ItemStack(Material.IRON_HOE);
		meta = wedge.getItemMeta();
		meta.setDisplayName("§rWedge");
		meta.setLore(Arrays.asList("§8A specialized club", "§8for tall obstacles."));
		meta.setCustomModelData(3);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, noDamage);
		meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, fastSwing);
		ShortUtils.addKey(meta, wedgeKey);
		wedge.setItemMeta(meta);

		recipe = new ShapedRecipe(wedgeKey, wedge);
		recipe.shape(" s", "is", " i");
		recipe.setIngredient('s', Material.STICK);
		recipe.setIngredient('i', Material.IRON_INGOT);
		Bukkit.addRecipe(recipe);

		// Putter item
		ItemStack putter = new ItemStack(Material.IRON_HOE);
		meta = putter.getItemMeta();
		meta.setDisplayName("§rPutter");
		meta.setLore(Arrays.asList("§8A specialized club", "§8for finishing holes."));
		meta.setCustomModelData(1);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, noDamage);
		meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, fastSwing);
		ShortUtils.addKey(meta, putterKey);
		putter.setItemMeta(meta);

		recipe = new ShapedRecipe(putterKey, putter);
		recipe.shape(" s", " s", "ii");
		recipe.setIngredient('s', Material.STICK);
		recipe.setIngredient('i', Material.IRON_INGOT);
		Bukkit.addRecipe(recipe);

		// Golf ball item
		golfBall = new ItemStack(Material.SNOWBALL);
		meta = golfBall.getItemMeta();
		meta.setDisplayName("§rGolf Ball");
		meta.setCustomModelData(1);
		ShortUtils.addKey(meta, ballKey);
		golfBall.setItemMeta(meta);

		recipe = new ShapedRecipe(ballKey, golfBall);
		recipe.shape(" p ", "pcp", " p ");
		recipe.setIngredient('p', Material.WHITE_DYE);
		recipe.setIngredient('c', Material.CLAY_BALL);
		Bukkit.addRecipe(recipe);

		// Whistle item
		ItemStack whistle = new ItemStack(Material.IRON_NUGGET);
		meta = whistle.getItemMeta();
		meta.setDisplayName("§rGolf Whistle");
		meta.setLore(Arrays.asList("§8Returns your last", "§8hit golf ball to its", "§8previous position."));
		meta.setCustomModelData(1);
		ShortUtils.addKey(meta, whistleKey);
		whistle.setItemMeta(meta);

		recipe = new ShapedRecipe(whistleKey, whistle);
		recipe.shape(" ii", "i i", " i ");
		recipe.setIngredient('i', Material.IRON_NUGGET);
		Bukkit.addRecipe(recipe);

		// Add keys to CraftLib if installed
		if (craftLib != null)
		{
			CraftLib cl = (CraftLib) craftLib;
			cl.addKey(ironKey);
			cl.addKey(putterKey);
			cl.addKey(wedgeKey);
			cl.addKey(ballKey);
			cl.addKey(whistleKey);
		}

		// Scheduler
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				// Iterate through golf balls
				Iterator<Snowball> i = golfBalls.iterator();
				while (i.hasNext())
				{
					// Get golf ball
					Snowball ball = i.next();
					if (!ball.isValid()) { i.remove(); continue; }

					// Drop if older than a minute
					if (ball.getTicksLived() > 1200)
					{
						ball.getWorld().dropItem(ball.getLocation(), golfBall);
						ball.remove();
						i.remove();
						return;
					}

					// Check block underneath
					Location loc = ball.getLocation();
					Block block = loc.subtract(0, 0.1, 0).getBlock();

					// Act upon block type
					Vector vel = ball.getVelocity();
					switch (block.getType())
					{
					case CAULDRON:
						// Check if golf ball is too fast
						if (vel.getY() >= 0 && vel.length() > 0.34)
						{
							return;
						}

						// Spawn firework
						Firework firework = (Firework) ball.getWorld().spawnEntity(ball.getLocation(), EntityType.FIREWORK);
						FireworkMeta meta = firework.getFireworkMeta();

						meta.setPower(1);
						meta.addEffect(fx);
						firework.setFireworkMeta(meta);

						// Scheduler
						new BukkitRunnable()
						{
							@Override
							public void run()
							{
								firework.detonate();
							}
						}.runTaskLater(JavaPlugin.getPlugin(Main.class), 20L);

						// Drop golf ball
						ball.getWorld().dropItem(ball.getLocation(), golfBall);

						// Send message
						for (Entry<UUID, Snowball> entry : lastPlayerBall.entrySet())
						{
							// Find the ball
							if (entry.getValue().equals(ball))
							{
								// Set up message
								int par = ball.getPersistentDataContainer().get(parKey, PersistentDataType.INTEGER);
								Player ply = Bukkit.getPlayer(entry.getKey());

								String msg = StringUtils.replaceEach(scoreMsg, tokens, new String[] { ply.getName(), Integer.toString(par) });

								// Alert player
								ply.sendMessage(msg);

								// Alert other nearby players
								for (Entity ent : ball.getNearbyEntities(20, 20, 20))
								{
									if (ent instanceof Player && !entry.getKey().equals(ent.getUniqueId()))
									{
										ply = (Player) ent;
										ply.sendMessage(msg);
									}
								}
								break;
							}
						}

						// Remove ball
						ball.remove();
						i.remove();
						break;

					case SOUL_SAND:
						// Halt velocity
						ball.setVelocity(new Vector(0, ball.getVelocity().getY(), 0));
					case AIR:
						// Fall
						ball.setGravity(true);
						break;

					case SLIME_BLOCK:
						// Bounce
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
						// Get direction
						Directional directional = (Directional) block.getBlockData();

						Vector newVel;
						switch (directional.getFacing())
						{
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

						// Push ball
						ball.setVelocity(vel.multiply(9.0).add(newVel).multiply(0.1));
						break;

						/*case HOPPER:
								// Transfer
								Hopper hopper = (Hopper) block.getState();
								Map<Integer, ItemStack> leftOver = hopper.getInventory().addItem(plugin.golfBall());

								// Check if added successfully
								if (leftOver.isEmpty())
								{
									// Remove ball
									ball.remove();
									i.remove();
								}
								break;*/

					default:
						// Check if floating above slabs
						if (isBottomSlab(block) && loc.getY() > block.getY() + 0.5)
						{
							// Fall
							ball.setGravity(true);
						}

						// Slight friction
						vel.multiply(0.975);
						ball.setVelocity(vel);
						break;
					}
				}
			}
		}.runTaskTimer(this, 0L, 1L);
	}

	@Override
	public void onDisable()
	{

	}

	public void reload()
	{
		reloadConfig();
		scoreMsg = ChatColor.translateAlternateColorCodes('&', getConfig().getString("scoreMsg"));
	}

	public ItemStack golfBall()
	{
		return golfBall;
	}

	public boolean isBottomSlab(Block block)
	{
		return Tag.SLABS.isTagged(block.getType()) && ((Slab) block.getBlockData()).getType() == Slab.Type.BOTTOM;
	}
}