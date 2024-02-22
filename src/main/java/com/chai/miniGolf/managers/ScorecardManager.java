package com.chai.miniGolf.managers;

import com.chai.miniGolf.models.Hole;
import com.chai.miniGolf.utils.ShortUtils.ShortUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

import static com.chai.miniGolf.Main.getPlugin;

public class ScorecardManager implements Listener {
    private static final NamespacedKey RANDOM_UUID_KEY = new NamespacedKey(getPlugin(), "random_uuid");

    @EventHandler
    private void onScorecheck(PlayerInteractEvent event) {
        GolfingCourseManager.GolfingInfo golferInfo = getPlugin().golfingCourseManager().getGolfingInfo(event.getPlayer());
        if (golferInfo == null
            || !(event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
            || event.getItem() == null
            || event.getItem().getItemMeta() == null
            || !ShortUtils.hasKey(event.getItem().getItemMeta(), getPlugin().scorecardKey)) {
            return;
        }
        int numHoles = golferInfo.getCourse().getHoles().size();
        int numRows = numHoles % 9 == 0 ? numHoles / 9 : numHoles / 9 + 1;
        Inventory scorecardInv = Bukkit.createInventory(null, numRows * 9, "Yard Golf - " + golferInfo.getCourse().getName());
        for (Hole hole : golferInfo.getCourse().getHoles()) {
            if (!hole.hasPlayerFinishedHole(event.getPlayer())) {
                continue;
            }
            int score = hole.playersScore(event.getPlayer());
            ItemStack holeItem = createHoleItem(hole, score);
            scorecardInv.addItem(holeItem);
        }
        event.getPlayer().openInventory(scorecardInv);
    }

    @EventHandler
    private void onScorecardInteract(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("Yard Golf - ")) {
            event.setCancelled(true);
        }
    }

    private ItemStack createHoleItem(Hole hole, int score) {
        Material holeMaterial;
        String holeResult;
        if (score == 1) {
            holeMaterial = Material.LIGHT_BLUE_CONCRETE;
            holeResult = String.format("%sHole in one!%s", ChatColor.AQUA, ChatColor.RESET);
        } else if (score < hole.getPar() - 2) {
            holeMaterial = Material.CYAN_CONCRETE;
            holeResult = String.format("%sEagle+!%s", ChatColor.GREEN, ChatColor.RESET);
        }  else if (score == hole.getPar() - 2) {
            holeMaterial = Material.LIME_CONCRETE;
            holeResult = String.format("%sEagle!%s", ChatColor.GREEN, ChatColor.RESET);
        } else if (score == hole.getPar() - 1) {
            holeMaterial = Material.GREEN_CONCRETE;
            holeResult = String.format("%sBirdie!%s", ChatColor.DARK_GREEN, ChatColor.RESET);
        } else if (score == hole.getPar()) {
            holeMaterial = Material.WHITE_CONCRETE;
            holeResult = String.format("%sPar%s", ChatColor.WHITE, ChatColor.RESET);
        } else if (score == hole.getPar() + 1) {
            holeMaterial = Material.PINK_CONCRETE;
            holeResult = String.format("%sBogey%s", ChatColor.RED, ChatColor.RESET);
        } else if (score == hole.getPar() + 2) {
            holeMaterial = Material.RED_CONCRETE;
            holeResult = String.format("%sDouble Bogey%s", ChatColor.DARK_RED, ChatColor.RESET);
        } else {
            holeMaterial = Material.BROWN_CONCRETE;
            holeResult = String.format("%sDouble Bogey+%s", ChatColor.DARK_GRAY, ChatColor.RESET);
        }
        ItemStack holeItem = new ItemStack(holeMaterial);
        ItemMeta meta = holeItem.getItemMeta();
        meta.setDisplayName(holeResult);
        meta.setLore(List.of("Par: " + hole.getPar(), "Your score: " + score));
        meta.getPersistentDataContainer().set(RANDOM_UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        holeItem.setItemMeta(meta);
        return holeItem;
    }
}
