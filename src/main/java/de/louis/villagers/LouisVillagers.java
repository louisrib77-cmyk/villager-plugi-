package de.louis.villagers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * - Villager-Trades sind unbegrenzt kaufbar (kein Aufbrauchen, keine Preisänderung).
 * - Das Angebot ändert sich nur, wenn der Arbeitsblock abgebaut und neu platziert wird.
 */
public final class LouisVillagers extends JavaPlugin implements Listener {

    private static final int MAX_USES = 1_000_000;

    private static final Set<Material> JOB_BLOCKS = EnumSet.of(
            Material.BARREL, Material.BLAST_FURNACE, Material.BREWING_STAND,
            Material.CARTOGRAPHY_TABLE, Material.CAULDRON, Material.WATER_CAULDRON,
            Material.LAVA_CAULDRON, Material.POWDER_SNOW_CAULDRON, Material.COMPOSTER,
            Material.FLETCHING_TABLE, Material.GRINDSTONE, Material.LECTERN,
            Material.LOOM, Material.SMITHING_TABLE, Material.SMOKER, Material.STONECUTTER
    );

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("LouisVillagers aktiv.");
    }

    /** Vor dem Öffnen des Handelsfensters: unbegrenzte Trades, feste Preise. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;

        List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
        if (recipes.isEmpty()) return;

        for (MerchantRecipe recipe : recipes) {
            recipe.setMaxUses(MAX_USES);
            recipe.setUses(0);
            recipe.setIgnoreDiscounts(true);
            recipe.setPriceMultiplier(0f);
        }
        villager.setRecipes(recipes);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        release(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().forEach(this::release);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().forEach(this::release);
    }

    /**
     * Wird ein Arbeitsblock abgebaut, verliert der zugehörige Villager Job, Level,
     * XP und Trades. Beim erneuten Platzieren sucht er sich den Job neu
     * und bekommt neue, zufällige Trades.
     */
    private void release(Block block) {
        if (!JOB_BLOCKS.contains(block.getType())) return;

        for (Entity entity : block.getWorld().getNearbyEntities(
                block.getLocation(), 64, 64, 64, e -> e instanceof Villager)) {
            Villager villager = (Villager) entity;
            Location site = villager.getMemory(MemoryKey.JOB_SITE);
            if (site == null || site.getWorld() == null || !site.getWorld().equals(block.getWorld())) continue;
            if (site.getBlockX() != block.getX() || site.getBlockY() != block.getY()
                    || site.getBlockZ() != block.getZ()) continue;

            villager.setRecipes(new ArrayList<>());
            villager.setVillagerExperience(0);
            villager.setVillagerLevel(1);
            villager.setMemory(MemoryKey.JOB_SITE, null);
            villager.setProfession(Villager.Profession.NONE);
        }
    }
}
