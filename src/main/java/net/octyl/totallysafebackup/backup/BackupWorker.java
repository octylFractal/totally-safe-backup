/*
 * This file is part of totally-safe-backup.
 *
 * Copyright (c) Octavia Togami <https://octyl.net>
 * Copyright (c) contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package net.octyl.totallysafebackup.backup;

import net.minecraft.Util;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.octyl.totallysafebackup.backup.target.BackupTarget;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker for backing up a server.
 */
public class BackupWorker {
    private static final Logger LOGGER = LogManager.getLogger();

    private final BackupState backupState;
    private final MinecraftServer server;

    /**
     * Create a new backup worker.
     *
     * @param server the server to backup
     */
    public BackupWorker(MinecraftServer server) {
        this.backupState = (BackupState) server;
        this.server = server;
    }

    /**
     * Run something on the server thread, and block for it to finish. Will throw exceptions from {@code runnable}.
     *
     * @param runnable the runnable to run
     */
    private void onServerThread(Runnable runnable) {
        server.executeBlocking(runnable);
    }

    /**
     * Responsible for backing up the server.
     *
     * @param backupTarget the directory to back up to, caller is responsible for cleanup on failure
     * @throws Exception if the backup fails
     */
    public void runBackupProcess(BackupTarget backupTarget) throws Exception {
        // Broadcast start
        server.getPlayerList().broadcastMessage(
            new TextComponent("Server backup started..."), ChatType.SYSTEM, Util.NIL_UUID
        );
        // Enter safe zone, where we will unset the backup flag when we're done
        var didSetBackingUp = new AtomicBoolean();
        try {
            // WORLD SYNC: Save the world, then pause it from saving
            onServerThread(() -> {
                if (server.isCurrentlySaving()) {
                    // This is apparently an async save. This shouldn't happen, but if it does, bail.
                    throw new IllegalStateException("Server is being saved asynchronously!");
                }
                if (backupState.isBackingUp()) {
                    // Unclean state from a previous backup. This shouldn't happen, but if it does, bail.
                    throw new IllegalStateException("Server is already backing up!");
                }
                // (1) log the progress, (2) flush to disk, (3) force saving
                if (!server.saveEverything(false, true, true)) {
                    throw new RuntimeException("Failed to save the world!");
                }
                backupState.setBackingUp(true);
                didSetBackingUp.set(true);
            });
            // Now we can actually backup the server
            backupServer(backupTarget);

            server.getPlayerList().broadcastMessage(
                new TextComponent("Server backup completed!"), ChatType.SYSTEM, Util.NIL_UUID
            );
        } catch (Throwable t) {
            server.getPlayerList().broadcastMessage(
                new TextComponent("Server backup failed! See console for details."), ChatType.SYSTEM, Util.NIL_UUID
            );
            LOGGER.warn("Server backup failed!", t);
            throw t;
        } finally {
            if (didSetBackingUp.get()) {
                backupState.setBackingUp(false);
            }
        }
    }

    private void backupServer(BackupTarget backupTarget) throws Exception {
        LOGGER.info("Backing up server into {}", backupTarget);
        var worldDir = backupState.getStorageAccess().getWorldDir();
        Files.walkFileTree(worldDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Skip the session lock file
                if (file.getFileName().toString().equals("session.lock")) {
                    return FileVisitResult.CONTINUE;
                }
                var relativePath = worldDir.relativize(file);
                backupTarget.saveFile(relativePath, stream -> Files.copy(file, stream));
                return FileVisitResult.CONTINUE;
            }
        });
        // Close it to save the file to disk.
        backupTarget.commit();
    }
}
