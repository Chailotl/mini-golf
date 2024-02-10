package com.chai.miniGolf.utils.ShortUtils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class ShortUtils
{
	public static void addKey(ItemMeta meta, NamespacedKey key)
	{
		meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 0);
	}

	public static boolean hasKey(ItemMeta meta, NamespacedKey key)
	{
		return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
	}

	private static List<Material> mats = Arrays.asList(
			Material.CRAFTING_TABLE, Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
			Material.BREWING_STAND, Material.GRINDSTONE, Material.ENCHANTING_TABLE, Material.STONECUTTER,
			Material.CARTOGRAPHY_TABLE, Material.LOOM, Material.BELL, Material.ANVIL, Material.CHIPPED_ANVIL,
			Material.DAMAGED_ANVIL, Material.LECTERN, Material.BEACON,

			Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST, Material.BARREL,
			Material.DISPENSER, Material.DROPPER, Material.HOPPER,

			Material.BLACK_BED, Material.BLUE_BED, Material.BROWN_BED, Material.CYAN_BED,
			Material.GRAY_BED, Material.GREEN_BED, Material.LIGHT_BLUE_BED, Material.LIGHT_GRAY_BED,
			Material.LIME_BED, Material.MAGENTA_BED, Material.ORANGE_BED, Material.PINK_BED,
			Material.PURPLE_BED, Material.RED_BED, Material.WHITE_BED, Material.YELLOW_BED,

			Material.BLACK_SHULKER_BOX, Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX, Material.CYAN_SHULKER_BOX,
			Material.GRAY_SHULKER_BOX, Material.GREEN_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX,
			Material.LIME_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.PINK_SHULKER_BOX,
			Material.PURPLE_SHULKER_BOX, Material.RED_SHULKER_BOX, Material.WHITE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX,

			Material.REPEATER, Material.COMPARATOR, Material.NOTE_BLOCK, Material.JUKEBOX, Material.DAYLIGHT_DETECTOR,

			Material.ACACIA_BUTTON, Material.BIRCH_BUTTON, Material.DARK_OAK_BUTTON, Material.JUNGLE_BUTTON,
			Material.OAK_BUTTON, Material.SPRUCE_BUTTON, Material.STONE_BUTTON, Material.LEVER,

			Material.ACACIA_FENCE_GATE, Material.BIRCH_FENCE_GATE, Material.DARK_OAK_FENCE_GATE,
			Material.JUNGLE_FENCE_GATE, Material.OAK_FENCE_GATE, Material.SPRUCE_FENCE_GATE,

			Material.ACACIA_DOOR, Material.BIRCH_DOOR, Material.DARK_OAK_DOOR,
			Material.JUNGLE_DOOR, Material.OAK_DOOR, Material.SPRUCE_DOOR,

			Material.ACACIA_TRAPDOOR, Material.BIRCH_TRAPDOOR, Material.DARK_OAK_TRAPDOOR,
			Material.JUNGLE_TRAPDOOR, Material.OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR
			);

	public static boolean interacting(PlayerInteractEvent event)
	{
		// We need to be right clicking a block to interact
		// Sneaking also cancels interactions
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getPlayer().isSneaking())
		{
			return false;
		}

		// Check if right clicking a universal interactible
		Block block = event.getClickedBlock();

		return mats.contains(block.getType());
	}

	public static void cancelOriginalTool(PlayerInteractEvent event)
	{
		// Is there an action we can cancel?
		Action act = event.getAction();
		if (act != Action.RIGHT_CLICK_AIR && act != Action.RIGHT_CLICK_BLOCK)
		{
			// Nothing to do here
			return;
		}

		// Get the tool in question
		Material tool = event.getItem().getType();
		Block block = event.getClickedBlock();
		Material mat = block != null ? block.getType() : null;

		switch (tool)
		{
		case WOODEN_HOE:
		case STONE_HOE:
		case IRON_HOE:
		case GOLDEN_HOE:
		case DIAMOND_HOE:
			if (mat == Material.DIRT || mat == Material.GRASS_BLOCK ||
			mat == Material.DIRT_PATH || mat == Material.COARSE_DIRT)
			{
				event.setCancelled(true);
			}
			break;

		case WOODEN_AXE:
		case STONE_AXE:
		case IRON_AXE:
		case GOLDEN_AXE:
		case DIAMOND_AXE:
			if (Tag.LOGS.isTagged(mat) || mat == Material.ACACIA_WOOD || mat == Material.BIRCH_WOOD ||
			mat == Material.DARK_OAK_WOOD || mat == Material.JUNGLE_WOOD ||
			mat == Material.OAK_WOOD || mat == Material.SPRUCE_WOOD)
			{
				event.setCancelled(true);
			}
			break;

		case WOODEN_SHOVEL:
		case STONE_SHOVEL:
		case IRON_SHOVEL:
		case GOLDEN_SHOVEL:
		case DIAMOND_SHOVEL:
			if (mat == Material.GRASS_BLOCK || mat == Material.CAMPFIRE)
			{
				event.setCancelled(true);
			}
			break;

		case LEAD:
			if (Tag.FENCES.isTagged(mat))
			{
				event.setCancelled(true);
			}
			break;

		case FISHING_ROD:
		case FLINT_AND_STEEL:
		case SNOWBALL:
		case ENDER_PEARL:
		case ENDER_EYE:
		case FIREWORK_ROCKET:
			event.setCancelled(true);
			break;

		default:
			break;
		}
	}
}