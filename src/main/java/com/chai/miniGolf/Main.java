package com.chai.miniGolf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

import com.chai.miniGolf.commands.CreateCourseCommand;
import com.chai.miniGolf.commands.EditCourseCommand;
import com.chai.miniGolf.commands.InfoCommand;
import com.chai.miniGolf.commands.PlayCommand;
import com.chai.miniGolf.commands.ReloadCommand;
import com.chai.miniGolf.configs.MiniGolfConfig;
import com.chai.miniGolf.utils.CraftLib.CraftLib;
import com.chai.miniGolf.utils.CraftLib.CraftingListener;
import com.chai.miniGolf.utils.ShortUtils.ShortUtils;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;

import static com.chai.miniGolf.utils.fileutils.FileUtils.loadConfig;

public class Main extends JavaPlugin {
	private static Main plugin;
	private static Logger logger;
	private static CraftLib craftLib = new CraftLib();
	private static MiniGolfConfig config;

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
	private final String[] tokens = new String[] { "&v1", "&v2" };

	@Override
	public void onEnable()
	{
		plugin = this;
		logger = this.getLogger();

		// Config
		reloadConfigs();
		registerCommands();
		registerListeners();
		registerItems();

		// Scheduler
		// TODO: Don't run unless a game is going on.
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

								String msg = StringUtils.replaceEach(config().scoreMsg(), tokens, new String[] { ply.getName(), Integer.toString(par) });

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

	private void registerItems() {
		// Iron item
		ItemStack iron = new ItemStack(Material.IRON_HOE);
		ItemMeta meta = iron.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "Iron");
		meta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "A well-rounded club", ChatColor.DARK_GRAY + "for longer distances."));
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
		meta.setDisplayName(ChatColor.RESET + "Wedge");
		meta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "A specialized club", ChatColor.DARK_GRAY + "for tall obstacles."));
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
		meta.setDisplayName(ChatColor.RESET + "Putter");
		meta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "A specialized club", ChatColor.DARK_GRAY + "for finishing holes."));
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
		meta.setDisplayName(ChatColor.RESET + "Golf Ball");
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
		meta.setDisplayName(ChatColor.RESET + "Golf Whistle");
		meta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "Returns your last", ChatColor.DARK_GRAY + "hit golf ball to its", ChatColor.DARK_GRAY + "previous position."));
		meta.setCustomModelData(1);
		ShortUtils.addKey(meta, whistleKey);
		whistle.setItemMeta(meta);

		recipe = new ShapedRecipe(whistleKey, whistle);
		recipe.shape(" ii", "i i", " i ");
		recipe.setIngredient('i', Material.IRON_NUGGET);
		Bukkit.addRecipe(recipe);

		// Add keys to CraftLib if installed
		craftLib.addKey(ironKey);
		craftLib.addKey(putterKey);
		craftLib.addKey(wedgeKey);
		craftLib.addKey(ballKey);
		craftLib.addKey(whistleKey);
	}

	private void registerListeners() {
		// Listeners
		getServer().getPluginManager().registerEvents(new PuttListener(), this);
		getServer().getPluginManager().registerEvents(new ProjectileListener(), this);
		getServer().getPluginManager().registerEvents(new UnloadListener(), this);
		getServer().getPluginManager().registerEvents(new CraftingListener(craftLib), this);
	}

	private void registerCommands() {
		// Register command
		this.getCommand("mgcreatecourse").setExecutor(new CreateCourseCommand());
		this.getCommand("mgedit").setExecutor(new EditCourseCommand());
		this.getCommand("mginfo").setExecutor(new InfoCommand());
		this.getCommand("mgplay").setExecutor(new PlayCommand());
		this.getCommand("mgreload").setExecutor(new ReloadCommand());
	}

	@Override
	public void onDisable()
	{

	}

	public MiniGolfConfig config() {
		return config;
	}

	public String reloadConfigs() {
		try {
			config = new MiniGolfConfig(loadConfig("config.yml"));
			return "Successfully loaded configs.";
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	public ItemStack golfBall()
	{
		return golfBall;
	}

	public boolean isBottomSlab(Block block)
	{
		return Tag.SLABS.isTagged(block.getType()) && ((Slab) block.getBlockData()).getType() == Slab.Type.BOTTOM;
	}

	public static Main getPlugin() {
		return plugin;
	}

	public static Logger logger() {
		return logger;
	}
}