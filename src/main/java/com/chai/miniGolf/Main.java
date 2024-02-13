package com.chai.miniGolf;

import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Logger;

import com.chai.miniGolf.commands.CreateCourseCommand;
import com.chai.miniGolf.commands.EditCourseCommand;
import com.chai.miniGolf.commands.InfoCommand;
import com.chai.miniGolf.commands.PlayCommand;
import com.chai.miniGolf.commands.ReloadCommand;
import com.chai.miniGolf.commands.TestCommand;
import com.chai.miniGolf.configs.MiniGolfConfig;
import com.chai.miniGolf.managers.GolfingCourseManager;
import com.chai.miniGolf.utils.CraftLib.CraftLib;
import com.chai.miniGolf.utils.CraftLib.CraftingListener;
import com.chai.miniGolf.utils.ShortUtils.ShortUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

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

	public final NamespacedKey strokesKey = new NamespacedKey(this, "strokesKey");
	public final NamespacedKey xKey = new NamespacedKey(this, "x");
	public final NamespacedKey yKey = new NamespacedKey(this, "y");
	public final NamespacedKey zKey = new NamespacedKey(this, "z");

	// Stuff
	private final AttributeModifier noDamage = new AttributeModifier(UUID.randomUUID(), "generic.attackDamage", -10, Operation.ADD_NUMBER, EquipmentSlot.HAND);
	private final AttributeModifier fastSwing = new AttributeModifier(UUID.randomUUID(), "generic.attackSpeed", 10, Operation.MULTIPLY_SCALAR_1, EquipmentSlot.HAND);

	// Constants
	public final double floorOffset = 0.05;

	// Items
	private ItemStack golfBall;

	// Managers
	private final GolfingCourseManager golfingCourseManager = new GolfingCourseManager();

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
		getServer().getPluginManager().registerEvents(golfingCourseManager, this);
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
		this.getCommand("tc").setExecutor(new TestCommand());
	}

	@Override
	public void onDisable() {

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

	public ItemStack golfBallItemStack() {
		return golfBall;
	}

	public static Main getPlugin() {
		return plugin;
	}

	public static Logger logger() {
		return logger;
	}

	public GolfingCourseManager golfingCourseManager() {
		return golfingCourseManager;
	}
}