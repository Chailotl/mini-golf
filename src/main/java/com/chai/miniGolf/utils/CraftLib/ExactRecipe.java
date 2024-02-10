package com.chai.miniGolf.utils.CraftLib;

import java.util.function.Consumer;

import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

public class ExactRecipe
{
	private NamespacedKey key;
	private ItemStack result;
	private Consumer<PrepareItemCraftEvent> callback = null;

	public ExactRecipe(NamespacedKey key, ItemStack result)
	{
		this.key = key;
		this.result = result;
	}

	public NamespacedKey getKey()
	{
		return key;
	}

	public ItemStack getResult()
	{
		return result;
	}

	/*
	 * You can optionally run a callback function
	 * from the PrepareItemCraftEvent event if the
	 * item can be crafted.
	 *
	 * @param callback the callback to run
	 */
	public void setCallback(Consumer<PrepareItemCraftEvent> callback)
	{
		this.callback = callback;
	}

	public void runCallback(PrepareItemCraftEvent event)
	{
		// Run if not null
		if (callback != null) { callback.accept(event); }
	}
}