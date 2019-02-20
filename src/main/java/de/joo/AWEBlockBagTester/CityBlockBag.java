package de.joo.AWEBlockBagTester;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.BlockBagException;
import com.sk89q.worldedit.extent.inventory.OutOfBlocksException;
import com.sk89q.worldedit.extent.inventory.UnplaceableBlockException;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class CityBlockBag extends BlockBag {
    private Map<Container, ItemStack[]> inventories = new HashMap<>();

    public CityBlockBag(Set<Container> chests) {
        chests.forEach(c -> inventories.put(c, null));
    }

    private void loadInventory() {
        if(inventories.isEmpty()) return;
        for(Map.Entry<Container, ItemStack[]> inv : inventories.entrySet()) {
            inv.getKey().getWorld().loadChunk(inv.getKey().getLocation().getChunk());
            if(inv.getValue() == null) {
                ItemStack[] is = inv.getKey().getInventory().getContents();
                inv.setValue(is);
            }
        }
    }

    @Override
    public void fetchBlock(BlockState blockState) throws BlockBagException {
        BlockType type = blockState.getBlockType();
        loadInventory();

        for (ItemStack[] items : inventories.values()) {
            for (int slot = 0; slot < items.length; ++slot) {
                ItemStack bukkitItem = items[slot];

                if (bukkitItem == null) {
                    continue;
                }

                if (!BukkitAdapter.equals(type, bukkitItem.getType())) {
                    // Type id doesn't fit
                    continue;
                }

                int currentAmount = bukkitItem.getAmount();
                if (currentAmount < 0) {
                    // Unlimited
                    return;
                }
                if (currentAmount > 1) {
                    //Bukkit.getLogger().info("old: " + entry.getKey().getInventory().getItem(slot).getAmount());
                    bukkitItem.setAmount(currentAmount - 1);
                    //Bukkit.getLogger().info("old: " + entry.getKey().getInventory().getItem(slot).getAmount());
                    return;
                } else {
                    items[slot] = null;
                    return;
                }
            }
        }
        Bukkit.getLogger().info("Throwing OutOfBlocksException, missing: " + type.getId());
        throw new OutOfBlocksException();
    }

    @Override
    public void storeBlock(BlockState blockState, int amount) throws BlockBagException {
        // Do nothing, we don't want to store blocks
    }

    @Override
    public void flushChanges() {
        for(Map.Entry<Container, ItemStack[]> inv : inventories.entrySet()) {
            if(inv.getValue() == null) continue;
            inv.getKey().getWorld().loadChunk(inv.getKey().getLocation().getChunk());
            inv.getKey().getInventory().setContents(inv.getValue());
            inv.setValue(null);
        }
    }

    @Override
    public void addSourcePosition(Location pos) {

    }

    @Override
    public void addSingleSourcePosition(Location pos) {

    }

}
