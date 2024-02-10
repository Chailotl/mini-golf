package com.chai.miniGolf.utils.CraftLib;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class CraftLib
{
	// Useful shortcuts
	public final Material[] banners = {
			Material.WHITE_BANNER, Material.ORANGE_BANNER, Material.MAGENTA_BANNER, Material.LIGHT_BLUE_BANNER,
			Material.YELLOW_BANNER, Material.LIME_BANNER, Material.PINK_BANNER, Material.GRAY_BANNER,
			Material.LIGHT_GRAY_BANNER, Material.CYAN_BANNER, Material.PURPLE_BANNER, Material.BLUE_BANNER,
			Material.BROWN_BANNER, Material.GREEN_BANNER, Material.RED_BANNER, Material.BLACK_BANNER
	};

	private List<ExactRecipe> recipes = new ArrayList<>();
	private Set<NamespacedKey> keys = new HashSet<>();

	public static class Pair
	{
		public final Material material;
		public final NamespacedKey key;

		public Pair(Material material, NamespacedKey key)
		{
			this.material = material;
			this.key = key;
		}
	}

	/*
	 * Adds custom recipes to CraftLib.
	 *
	 * @param recipe the custom recipe to be added
	 */
	public void addRecipe(ExactShapedRecipe recipe)
	{
		recipes.add(recipe);
		addKey(recipe.getKey());

		// We'll let Bukkit tell us if the recipe has been formed, then we decide which one it is
		ShapedRecipe bukkitRecipe = new ShapedRecipe(recipe.getKey(), recipe.getResult());
		bukkitRecipe.shape(recipe.getShape());
		for (Map.Entry<Character, CraftLib.Pair> entry : recipe.getIngredientMap().entrySet())
		{
			bukkitRecipe.setIngredient(entry.getKey(), entry.getValue().material);

			// Remember custom keys
			addKey(entry.getValue().key);
		}
		Bukkit.addRecipe(bukkitRecipe);
	}

	/*
	 * Adds custom recipes to CraftLib.
	 *
	 * @param recipe the custom recipe to be added
	 */
	public void addRecipe(ExactShapelessRecipe recipe)
	{
		recipes.add(recipe);
		addKey(recipe.getKey());

		// We'll let Bukkit tell us if the recipe has been formed, then we decide which one it is
		ShapelessRecipe bukkitRecipe = new ShapelessRecipe(recipe.getKey(), recipe.getResult());
		for (Pair pair : recipe.getIngredientList())
		{
			bukkitRecipe.addIngredient(pair.material);

			// Remember custom keys
			addKey(pair.key);
		}
		Bukkit.addRecipe(bukkitRecipe);
	}

	/*
	 * If you want custom items to be treated separately from
	 * regular items in crafting recipes, you can add its
	 * NamespacedKey to CraftLib.
	 *
	 * Custom items found in recipes added to CraftLib will be
	 * automatically handled.
	 *
	 * @param key the custom item's namespaced key
	 */
	public void addKey(NamespacedKey key)
	{
		if (key != null) { keys.add(key); }
	}

	public List<ExactRecipe> getRecipes()
	{
		return ImmutableList.copyOf(recipes);
	}

	public Set<NamespacedKey> getKeys()
	{
		return ImmutableSet.copyOf(keys);
	}
}