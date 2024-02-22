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
        int currentScore = golferInfo.getCourse().playerTotalScore(event.getPlayer());
        GolfingCourseManager.GolfingInfo golfingInfo = getPlugin().golfingCourseManager().getGolfingInfo(event.getPlayer());
        if (golfingInfo != null && golfingInfo.getGolfball() != null) {
            currentScore += golfingInfo.getGolfball().getPersistentDataContainer().get(getPlugin().strokesKey, PersistentDataType.INTEGER);
        }
        Inventory scorecardInv = Bukkit.createInventory(null, (numRows+1) * 9, String.format("Yard Golf - %s", golferInfo.getCourse().getName()));
        scorecardInv.setItem(3, createCourseParItem(golfingInfo.getCourse().totalPar()));
        scorecardInv.setItem(5, createCurrentScoreItem(currentScore));
        int invSpot = 9;
        for (Hole hole : golferInfo.getCourse().getHoles()) {
            if (!hole.hasPlayerFinishedHole(event.getPlayer()) && hole.playersScore(event.getPlayer()) != null) {
                if (golfingInfo != null && golfingInfo.getGolfball() != null) {
                    int score = golfingInfo.getGolfball().getPersistentDataContainer().get(getPlugin().strokesKey, PersistentDataType.INTEGER);
                    ItemStack holeItem = createInProgressHoleItem(hole, score);
                    scorecardInv.setItem(invSpot, holeItem);
                    invSpot++;
                }
            } else if (hole.hasPlayerFinishedHole(event.getPlayer())) {
                int score = hole.playersScore(event.getPlayer());
                ItemStack holeItem = createCompletedHoleItem(hole, score);
                scorecardInv.setItem(invSpot, holeItem);
                invSpot++;
            }
        }
        event.getPlayer().openInventory(scorecardInv);
    }

    private ItemStack createCurrentScoreItem(int currentScore) {
        ItemStack holeItem = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = holeItem.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Your Current Score: " + currentScore);
        meta.getPersistentDataContainer().set(RANDOM_UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        holeItem.setItemMeta(meta);
        return holeItem;
    }

    private ItemStack createCourseParItem(int par) {
        ItemStack holeItem = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = holeItem.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Course Par: " + par);
        meta.getPersistentDataContainer().set(RANDOM_UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        holeItem.setItemMeta(meta);
        return holeItem;
    }

    @EventHandler
    private void onScorecardInteract(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("Yard Golf - ")) {
            event.setCancelled(true);
        }
    }

    private ItemStack createInProgressHoleItem(Hole hole, int score) {
        ItemStack holeItem = new ItemStack(Material.FEATHER);
        ItemMeta meta = holeItem.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "(In Progress)");
        meta.setLore(List.of(ChatColor.WHITE + "Par: " + hole.getPar(),ChatColor.WHITE + "Your score: " + score));
        meta.getPersistentDataContainer().set(RANDOM_UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        holeItem.setItemMeta(meta);
        return holeItem;
    }

    private ItemStack createCompletedHoleItem(Hole hole, int score) {
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
        meta.setLore(List.of(ChatColor.WHITE + "Par: " + hole.getPar(),ChatColor.WHITE + "Your score: " + score));
        meta.getPersistentDataContainer().set(RANDOM_UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        holeItem.setItemMeta(meta);
        return holeItem;
    }
}
