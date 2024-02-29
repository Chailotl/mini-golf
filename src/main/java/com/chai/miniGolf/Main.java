package com.chai.miniGolf;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import com.chai.miniGolf.commands.CreateCourseCommand;
import com.chai.miniGolf.commands.DeleteCourseCommand;
import com.chai.miniGolf.commands.EditCourseCommand;
import com.chai.miniGolf.commands.InfoCommand;
import com.chai.miniGolf.commands.PlayCommand;
import com.chai.miniGolf.commands.ReloadCommand;
import com.chai.miniGolf.configs.MiniGolfConfig;
import com.chai.miniGolf.managers.GolfingCourseManager;
import com.chai.miniGolf.managers.LeaderboardManager;
import com.chai.miniGolf.managers.ScorecardManager;
import com.chai.miniGolf.models.LeaderboardsPAPIExpansion;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

import static com.chai.miniGolf.utils.fileutils.FileUtils.loadConfig;

public class Main extends JavaPlugin {
	private static Main plugin;
	private static Logger logger;
	private static MiniGolfConfig config;

	// NamespacedKeys (also used in the config.yml. Don't change these names punk)
	public final NamespacedKey ballKey = new NamespacedKey(this, "golf_ball");
	public final NamespacedKey putterKey = new NamespacedKey(this, "putter");
	public final NamespacedKey ironKey = new NamespacedKey(this, "iron");
	public final NamespacedKey wedgeKey = new NamespacedKey(this, "wedge");
	public final NamespacedKey whistleKey = new NamespacedKey(this, "return_whistle");
	public final NamespacedKey scorecardKey = new NamespacedKey(this, "scorecard");
	public final NamespacedKey nextHoleItemKey = new NamespacedKey(this, "next_hole_item");
	public final NamespacedKey hideOthersItemKey = new NamespacedKey(this, "hide_others_item");
	public final NamespacedKey quitItemKey = new NamespacedKey(this, "quit_item");

	public final NamespacedKey strokesKey = new NamespacedKey(this, "strokesKey");
	public final NamespacedKey ownerNameKey = new NamespacedKey(this, "ownerNameKey");
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
	private ItemStack putter;
	private ItemStack wedge;
	private ItemStack iron;
	private ItemStack whistle;
	private ItemStack scorecard;
	private ItemStack nextHoleItem;
	private ItemStack hideOthersItem;
	private ItemStack quitItem;

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
		registerPlaceholderApi();
	}

	private void registerPlaceholderApi() {
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			new LeaderboardsPAPIExpansion().register();
		}
	}

	private void registerItems() {
		// Iron item
		iron = new ItemStack(Material.IRON_HOE);
		ItemMeta meta = iron.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "Iron");
		meta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "A well-rounded club", ChatColor.DARK_GRAY + "for longer distances."));
		meta.setCustomModelData(2);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, noDamage);
		meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, fastSwing);
		ShortUtils.addKey(meta, ironKey);
		iron.setItemMeta(meta);

		// Wedge item
		wedge = new ItemStack(Material.IRON_HOE);
		meta = wedge.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "Wedge");
		meta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "A specialized club", ChatColor.DARK_GRAY + "for tall obstacles."));
		meta.setCustomModelData(3);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, noDamage);
		meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, fastSwing);
		ShortUtils.addKey(meta, wedgeKey);
		wedge.setItemMeta(meta);

		// Putter item
		putter = new ItemStack(Material.IRON_HOE);
		meta = putter.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "Putter");
		meta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "A specialized club", ChatColor.DARK_GRAY + "for finishing holes."));
		meta.setCustomModelData(1);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, noDamage);
		meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, fastSwing);
		ShortUtils.addKey(meta, putterKey);
		putter.setItemMeta(meta);

		// Golf ball item
		golfBall = new ItemStack(Material.SNOWBALL);
		meta = golfBall.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "Golf Ball");
		meta.setCustomModelData(1);
		ShortUtils.addKey(meta, ballKey);
		golfBall.setItemMeta(meta);

		// Whistle item
		whistle = new ItemStack(Material.IRON_NUGGET);
		meta = whistle.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "Whistle");
		meta.setLore(Arrays.asList(ChatColor.DARK_GRAY + "Returns your golf ball", ChatColor.DARK_GRAY + "to its previous position", ChatColor.DARK_GRAY + "(incurs a 1 stroke penalty)."));
		meta.setCustomModelData(1);
		ShortUtils.addKey(meta, whistleKey);
		whistle.setItemMeta(meta);

		// Scorecard item
		scorecard = new ItemStack(Material.BOOK);
		meta = scorecard.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "Scorecard");
		meta.setLore(List.of(ChatColor.DARK_GRAY + "Use this item to", ChatColor.DARK_GRAY + "view your current score"));
		ShortUtils.addKey(meta, scorecardKey);
		scorecard.setItemMeta(meta);

		// Next Hole Item
		nextHoleItem = new ItemStack(Material.ENDER_PEARL);
		meta = nextHoleItem.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "Next hole");
		meta.setLore(List.of(ChatColor.DARK_GRAY + "Use this item to", ChatColor.DARK_GRAY + "go to the next hole"));
		ShortUtils.addKey(meta, nextHoleItemKey);
		nextHoleItem.setItemMeta(meta);

		// Next Hole Item
		hideOthersItem = new ItemStack(Material.LIME_DYE);
		meta = hideOthersItem.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "Toggle Visibility");
		meta.setLore(List.of(ChatColor.DARK_GRAY + "Use this item to", ChatColor.DARK_GRAY + "hide or reveal other players"));
		ShortUtils.addKey(meta, hideOthersItemKey);
		hideOthersItem.setItemMeta(meta);

		// Quit Item
		quitItem = new ItemStack(Material.RED_BED);
		meta = quitItem.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "Quit");
		meta.setLore(List.of(ChatColor.DARK_GRAY + "Use this item to exit", ChatColor.DARK_GRAY + "the current course"));
		ShortUtils.addKey(meta, quitItemKey);
		quitItem.setItemMeta(meta);
	}

	private void registerListeners() {
		// Listeners
		getServer().getPluginManager().registerEvents(golfingCourseManager, this);
		getServer().getPluginManager().registerEvents(new PuttListener(), this);
		getServer().getPluginManager().registerEvents(new ProjectileListener(), this);
		getServer().getPluginManager().registerEvents(new UnloadListener(), this);
		getServer().getPluginManager().registerEvents(new ScorecardManager(), this);
		getServer().getPluginManager().registerEvents(new LeaderboardManager(), this);
	}

	private void registerCommands() {
		// Register command
		this.getCommand("mgcreatecourse").setExecutor(new CreateCourseCommand());
		this.getCommand("mgdeletecourse").setExecutor(new DeleteCourseCommand());
		this.getCommand("mgedit").setExecutor(new EditCourseCommand());
		this.getCommand("mginfo").setExecutor(new InfoCommand());
		this.getCommand("mgplay").setExecutor(new PlayCommand());
		this.getCommand("mgreload").setExecutor(new ReloadCommand());
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

	public ItemStack ironItemStack() {
		return iron;
	}

	public ItemStack wedgeItemStack() {
		return wedge;
	}

	public ItemStack whistleItemStack() {
		return whistle;
	}

	public ItemStack putterItemStack() {
		return putter;
	}

	public ItemStack scorecardItemStack() {
		return scorecard;
	}

	public ItemStack nextHoleItemItemStack() {
		return nextHoleItem;
	}

	public ItemStack hideOthersItemItemStack() {
		return hideOthersItem;
	}

	public ItemStack quitItemItemStack() {
		return quitItem;
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