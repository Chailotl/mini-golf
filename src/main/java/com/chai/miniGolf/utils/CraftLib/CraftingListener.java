package com.chai.miniGolf.utils.CraftLib;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class CraftingListener implements Listener
{
	private final CraftLib craftLib;

    public CraftingListener(CraftLib craftLib) {
        this.craftLib = craftLib;
    }

    private boolean isSameItem(ItemStack item, CraftLib.Pair pair)
	{
		// Check for identical material
		if (item.getType() == pair.material)
		{
			// Check for keys
			PersistentDataContainer c = item.getItemMeta().getPersistentDataContainer();

			if (pair.key != null)
			{
				return c.has(pair.key, PersistentDataType.BYTE);
			}
			else
			{
				// Check that item has no keys
				for (NamespacedKey k : craftLib.getKeys())
				{
					if (c.has(k, PersistentDataType.BYTE))
					{
						return false;
					}
				}
			}

			return true;
		}

		return false;
	}

	@EventHandler
	public void onCraft(PrepareItemCraftEvent event)
	{
		// We need a recipe
		if (event.getRecipe() == null) { return; }

		// Is it a CraftLib recipe?
		NamespacedKey key = ((Keyed) event.getRecipe()).getKey();
		ExactRecipe recipe = null;

		for (ExactRecipe r : craftLib.getRecipes())
		{
			if (key.equals(r.getKey()))
			{
				// Yes it is a CraftLib recipe
				recipe = r;
				break;
			}
		}

		// Iterate through slots
		ItemStack[] slots = event.getInventory().getMatrix();

		if (recipe == null)
		{
			for (ItemStack slot : slots)
			{
				// Ignore empty slots
				if (slot == null) { continue; }

				// Check if item has any custom item keys
				PersistentDataContainer c = slot.getItemMeta().getPersistentDataContainer();
				for (NamespacedKey k : craftLib.getKeys())
				{
					if (c.has(k, PersistentDataType.BYTE))
					{
						// Custom item key found
						event.getInventory().setResult(new ItemStack(Material.AIR));
						return;
					}
				}
			}
		}
		else if (recipe instanceof ExactShapelessRecipe)
		{
			List<CraftLib.Pair> ingredients = new ArrayList<>(((ExactShapelessRecipe) recipe).getIngredientList());

			for (ItemStack slot : slots)
			{
				// Ignore empty slots
				if (slot != null)
				{
					// Iterate through ingredients
					Iterator<CraftLib.Pair> i = ingredients.iterator();
					while (i.hasNext())
					{
						// Remove if it is the same item
						if (isSameItem(slot, i.next()))
						{
							i.remove();
						}
					}
				}
			}

			// If list is empty in the end then we have ourselves a valid recipe
			if (ingredients.size() == 0)
			{
				// Run callback
				recipe.runCallback(event);
			}
			else
			{
				event.getInventory().setResult(new ItemStack(Material.AIR));
			}
		}
		else
		{
			// Getting basic info about the recipe
			String[] shape = ((ExactShapedRecipe) recipe).getShape();
			Map<Character, CraftLib.Pair> ingredients = ((ExactShapedRecipe) recipe).getIngredientMap();
			int width = shape[0].length();
			int height = shape.length;
			int grid = slots.length == 9 ? 3 : 2;
			boolean match = false;

			// Instead of writing a function, I'm using a for loop to flip
			// shape[] since Minecraft recipes can be mirrored vertically
			for (int f = 0; f < 2; ++f)
			{
				// Flip on the second iteration
				if (f == 1)
				{
					// Ignore symmetrical recipes
					if (width == 1) { break; }

					for (int s = 0; s < height; ++s)
					{
						shape[s] = new StringBuilder(shape[s]).reverse().toString();
					}
				}

				// Iterate through each possible starting slot of the crafting grid
				for (int x = 0; x <= grid - width; ++x)
				{
					for (int y = 0; y <= grid - height; ++y)
					{
						// Testing each corner spot
						match = true;

						for (int i = 0; i < width; ++i)
						{
							// We can only break out of one loop at a
							// time, so this is to skip the outer loop
							if (!match) { break; }
							for (int j = 0; j < height; ++j)
							{
								// We need to translate 2D to 1D because slots[] is 1D
								ItemStack slot = slots[(x + i) + (y + j) * grid];
								Character c = shape[j].charAt(i);

								// Testing for empty slots
								if (c == ' ' || slot == null)
								{
									if (!(c == ' ' && slot == null))
									{
										// Discrepency found!
										match = false;
										break;
									}
								}
								else if (!isSameItem(slot, ingredients.get(c)))
								{
									// Test if the items are the same
									match = false;
									break;
								}
							}
						}

						// If still true then a match was found and we can stop here
						if (match)
						{
							// Run callback
							recipe.runCallback(event);
							return;
						}
					}
				}
			}

			// No match was found, so we have to clear the output
			if (!match)
			{
				event.getInventory().setResult(new ItemStack(Material.AIR));
			}
		}
	}
}