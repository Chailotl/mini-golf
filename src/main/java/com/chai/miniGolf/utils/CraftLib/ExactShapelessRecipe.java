package com.chai.miniGolf.utils.CraftLib;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.ImmutableList;

public class ExactShapelessRecipe extends ExactRecipe
{
	private List<CraftLib.Pair> ingredients = new ArrayList<>();

	public ExactShapelessRecipe(NamespacedKey key, ItemStack result)
	{
		super(key, result);
	}

	/*
	 * @param ingredient the material type
	 */
	public void addIngredient(Material ingredient)
	{
		addIngredient(ingredient, null);
	}

	/*
	 * If the ingredient is a custom item, you can specify its
	 * namespaced key to differentiate it in recipes.
	 *
	 * @param ingredient the material type
	 * @param key the custom item's namespaced key
	 */
	public void addIngredient(Material ingredient, NamespacedKey key)
	{
		// Maximum crafting grid of 9 slots
		if (ingredients.size() < 9)
		{
			ingredients.add(new CraftLib.Pair(ingredient, key));
		}
	}

	public List<CraftLib.Pair> getIngredientList()
	{
		return ImmutableList.copyOf(ingredients);
	}
}