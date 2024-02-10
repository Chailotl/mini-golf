package com.chai.miniGolf.utils.CraftLib;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.ImmutableMap;

public class ExactShapedRecipe extends ExactRecipe
{
	private String[] shape;
	private Map<Character, CraftLib.Pair> ingredients = new HashMap<>();

	public ExactShapedRecipe(NamespacedKey key, ItemStack result)
	{
		super(key, result);
	}

	public CraftLib.Pair getIngredient(char c)
	{
		return ingredients.get(c);
	}

	public String[] getShape()
	{
		return shape;
	}

	/*
	 * @param c the character to represent the ingredient
	 * @param ingredient the material type
	 */
	public void setIngredient(char c, Material ingredient)
	{
		setIngredient(c, ingredient, null);
	}

	/*
	 * If the ingredient is a custom item, you can specify its
	 * namespaced key to differentiate it in recipes.
	 *
	 * @param c the character to represent the ingredient
	 * @param ingredient the material type
	 * @param key the custom item's namespaced key
	 */
	public void setIngredient(char c, Material ingredient, NamespacedKey key)
	{
		ingredients.put(c, new CraftLib.Pair(ingredient, key));
	}

	/*
	 * @param shape the shape of the crafting recipe
	 */
	public void setShape(String ... shape)
	{
		this.shape = shape;
	}

	public Map<Character, CraftLib.Pair> getIngredientMap()
	{
		return ImmutableMap.copyOf(ingredients);
	}
}