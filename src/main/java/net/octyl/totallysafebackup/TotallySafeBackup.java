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

package net.octyl.totallysafebackup;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.commands.Commands;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.octyl.totallysafebackup.backup.BackupWorker;
import net.octyl.totallysafebackup.backup.target.ZipBackupTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipOutputStream;

/**
 * Primary mod class.
 */
@Mod(TotallySafeBackup.MOD_ID)
public class TotallySafeBackup {
    /**
     * The mod's ID.
     */
    public static final String MOD_ID = "totally_safe_backup";
    /**
     * The mod's logger.
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(TotallySafeBackup.class);

    private static final DateTimeFormatter FILE_SAFE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH_mm_ss");

    private static final ScheduledExecutorService BACKUP_EXECUTOR =
        Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("totally-safe-backup-thread-%d")
            .build());

    private final Path backupDir;

    private ScheduledFuture<?> backupTask;

    /**
     * Create a new instance of the mod.
     */
    public TotallySafeBackup() {
        this.backupDir = FMLPaths.getOrCreateGameRelativePath(
            Path.of("totally-safe-backups"),
            "totally safe backup directory"
        );

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(FMLCommonSetupEvent event) {
        LOGGER.info("Where did I put that world folder... oh, it's right there! I'll keep it safe, I promise!");
    }

    @SubscribeEvent
    public void commandRegistration(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("totally-safe-backup")
                .requires(ctx -> ctx.hasPermission(2))
                .then(Commands.literal("perform").executes(
                    ctx -> performBackup(new BackupWorker(ctx.getSource().getServer())) ? 1 : 0
                ))
        );
    }

    @SubscribeEvent
    public void serverStarted(ServerStartedEvent event) {
        LOGGER.info("Server started, starting backup worker...");
        var worker = new BackupWorker(event.getServer());
        backupTask = BACKUP_EXECUTOR.scheduleAtFixedRate(
            () -> performBackup(worker), 30, 30, TimeUnit.MINUTES
        );
    }

    private boolean performBackup(BackupWorker worker) {
        var timestamp = ZonedDateTime.now(ZoneOffset.UTC);
        var targetZipPath = backupDir.resolve(FILE_SAFE_DATE_FORMAT.format(timestamp) + ".zip");
        try (var targetZip = new ZipOutputStream(Files.newOutputStream(targetZipPath))) {
            worker.runBackupProcess(new ZipBackupTarget(targetZipPath.toString(), targetZip));
            return true;
        } catch (Throwable mainThrowable) {
            LOGGER.warn("Failed to perform backup", mainThrowable);
            try {
                Files.deleteIfExists(targetZipPath);
            } catch (Throwable duringDelete) {
                LOGGER.warn("Failed to delete backup file after failure", duringDelete);
            }
            // Terminate ourselves if we got an Error
            if (mainThrowable instanceof Error err) {
                throw err;
            }
            return false;
        }
    }

    @SubscribeEvent
    public void serverStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping, cancelling backup worker...");
        backupTask.cancel(true);
        backupTask = null;
    }
}
