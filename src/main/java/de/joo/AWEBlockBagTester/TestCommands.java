package de.joo.AWEBlockBagTester;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class TestCommands implements CommandExecutor {
    private SchematicManager manager;

    public TestCommands(TestPlugin plugin) {
        manager = new SchematicManager(plugin.getDataFolder());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 0) {
            sender.sendMessage("/tester save OR /tester load");
            return true;
        }
        if(!(sender instanceof Player)) {
            sender.sendMessage("Player only.");
            return true;
        }
        Player player = (Player)sender;
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        ApplicableRegionSet applicableRegions = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
        if(applicableRegions.getRegions().isEmpty()) {
            sender.sendMessage("Stay inside worldguard region please.");
            return true;
        }
        ProtectedRegion region = applicableRegions.getRegions().iterator().next();
        String path = region.getId() + ".schematic";
        if(args[0].equalsIgnoreCase("save")) {
            save(player, region, path);
        } else if(args[0].equalsIgnoreCase("load")) {
            load(player, region, path);
        }
        return true;
    }

    private void save(Player player, ProtectedRegion region, String path) {
        World weWorld = BukkitAdapter.adapt(player.getWorld());
        Region weRegion = toRegion(weWorld, region);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(weRegion);
        EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1);
        ForwardExtentCopy copy = new ForwardExtentCopy(session, weRegion, clipboard, region.getMinimumPoint());
        copy.setCopyingEntities(false);
        try {
            Operations.completeLegacy(copy);
            manager.saveSubRegionSchematic(path, clipboard);
        } catch (MaxChangedBlocksException | IOException e) {
            player.sendMessage("Error while saving Region.");
            e.printStackTrace();
            return;
        }
        player.sendMessage("Success.");
    }

    private void load(Player player, ProtectedRegion region, String path) {
        BlockBag bag = new CityBlockBag(Collections.emptySet());
        if(!manager.hasSchematic(path)) {
            player.sendMessage("No schematic found.");
            return;
        }
        Clipboard clipboard;
        try {
            clipboard = manager.loadSchematic(path);
        } catch (IOException e) {
            player.sendMessage("Error loading Schematic.");
            e.printStackTrace();
            return;
        }
        AsyncWorldEditQueue queue = new AsyncWorldEditQueue(player, BukkitAdapter.adapt(player.getWorld())
                , "loading " + region.getId(), bag);
        PasteAction action = new PasteAction(region.getMinimumPoint(), new ClipboardHolder(clipboard), false);
        queue.addJob(action);
        queue.start();
        player.sendMessage("Job started.");
        queue.getFuture().thenAccept(aBoolean -> {
            Map<BlockType, Integer> missing = queue.getEditSession().popMissingBlocks();
            if(missing.isEmpty()) {
                player.sendMessage("No missing mats, success.");
                return;
            }
            player.sendMessage("Missing " + missing.size() + " different types:");
            missing.forEach((blockType, integer) ->
                    player.sendMessage("Type: " + blockType.getId() + " - amount: " + integer)
            );
        });

    }

    public static Region toRegion(World world, ProtectedRegion region) {
        if (region instanceof ProtectedCuboidRegion) {
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            return new CuboidRegion(world, min, max);
        } else if (region instanceof ProtectedPolygonalRegion) {
            return new Polygonal2DRegion(world, region.getPoints(),
                    region.getMinimumPoint().getY(), region.getMaximumPoint().getY());
        } else {
            throw new RuntimeException("Unknown region type: " + region.getClass().getCanonicalName());
        }
    }
}
