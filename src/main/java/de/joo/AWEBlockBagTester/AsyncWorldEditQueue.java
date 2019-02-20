/*
 * Copyright (c) 2016. Starlis LLC / dba Empire Minecraft
 *
 * This source code is proprietary software and must not be redistributed without Starlis LLC's approval
 *
 */

package de.joo.AWEBlockBagTester;

import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.primesoft.asyncworldedit.AsyncWorldEditMain;
import org.primesoft.asyncworldedit.api.blockPlacer.IBlockPlacer;
import org.primesoft.asyncworldedit.api.blockPlacer.IBlockPlacerListener;
import org.primesoft.asyncworldedit.api.blockPlacer.IJobEntryListener;
import org.primesoft.asyncworldedit.api.blockPlacer.entries.IJobEntry;
import org.primesoft.asyncworldedit.api.blockPlacer.entries.JobStatus;
import org.primesoft.asyncworldedit.api.playerManager.IPlayerEntry;
import org.primesoft.asyncworldedit.api.utils.IFuncParamEx;
import org.primesoft.asyncworldedit.api.worldedit.IAsyncEditSessionFactory;
import org.primesoft.asyncworldedit.api.worldedit.ICancelabeEditSession;
import org.primesoft.asyncworldedit.api.worldedit.IThreadSafeEditSession;

import java.util.concurrent.CompletableFuture;

public class AsyncWorldEditQueue {

    private final IPlayerEntry player;
    private final CompletableFuture<Boolean> done;
    private final IThreadSafeEditSession editSession;
    private final IBlockPlacer placer;
    private final String id;
    private final StatusListener listener;

    public AsyncWorldEditQueue(Player player, World target, String displayName) {
        final AsyncWorldEditMain awe = AsyncWorldEditMain.getInstance();
        this.player = awe.getPlayerManager().getPlayer(player.getUniqueId());
        IAsyncEditSessionFactory factory = (IAsyncEditSessionFactory) WorldEdit.getInstance().getEditSessionFactory();
        this.editSession = factory.getThreadSafeEditSession(target, -1, BukkitAdapter.adapt(player));
        this.id = displayName;
        this.done = new CompletableFuture<>();
        this.placer = awe.getBlockPlacer();
        this.placer.setPause(true);
        this.listener = new StatusListener(id);
        this.placer.addListener(listener);
    }

    public AsyncWorldEditQueue(Player player, World world, String displayName, BlockBag bag) {
        final AsyncWorldEditMain awe = AsyncWorldEditMain.getInstance();
        this.player = awe.getPlayerManager().getUnknownPlayer();
        IAsyncEditSessionFactory factory = (IAsyncEditSessionFactory) WorldEdit.getInstance().getEditSessionFactory();
        this.editSession = // factory.getThreadSafeEditSession(world, -1, bag, BukkitAdapter.adapt(player));
            factory.getThreadSafeEditSession(world, -1);
        this.editSession.setBlockBag(bag);
        this.id = displayName;
        this.done = new CompletableFuture<>();

        this.placer = awe.getBlockPlacer();
        this.placer.setPause(true);
        this.listener = new StatusListener(id);
        this.placer.addListener(listener);
        getFuture().thenApply(aBoolean -> {
            this.placer.removeListener(this.listener);
            return aBoolean;
        });
    }

    public void addJob(IFuncParamEx<Integer,ICancelabeEditSession,MaxChangedBlocksException> job) {
        if (!this.placer.isPaused()) {
            throw new IllegalStateException("Already started");
        }
        this.placer.performAsAsyncJob(editSession, player, id, job);
    }

    public void setBlockMask(Mask mask) {
        this.editSession.setMask(mask);
    }

    public IThreadSafeEditSession getEditSession() {
        return this.editSession;
    }

    public void start() {
        this.placer.setPause(false);
    }

    public CompletableFuture<Boolean> getFuture() {
        return done;
    }

    private class StatusListener implements IBlockPlacerListener {

        private final String id;
        private final IJobEntryListener jobEntryListener;

        private StatusListener(String id) {
            this.id = id;
            this.jobEntryListener = job -> {
                if (!job.getName().equalsIgnoreCase(id)) {
                    return;
                }

                JobStatus status = job.getStatus();

                if (status == JobStatus.Done || status == JobStatus.Canceled) {
                    done.complete(status == JobStatus.Done);
                }
            };

        }

        @Override
        public void jobRemoved(IJobEntry job) {
            if(job.getName().equalsIgnoreCase(id)) {
                job.removeStateChangedListener(jobEntryListener);
                // AsyncWorldEditQueue.this.placer.removeListener(AsyncWorldEditQueue.this.listener);
            }
        }

        @Override
        public void jobAdded(IJobEntry job) {
            if(job.getName().equalsIgnoreCase(id)) {
                job.addStateChangedListener(jobEntryListener);
            }
        }
    }
}
