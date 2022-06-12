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

package net.octyl.totallysafebackup.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import net.octyl.totallysafebackup.TotallySafeBackup;
import net.octyl.totallysafebackup.backup.BackupState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;

@Mixin(ServerLevel.class)
public abstract class SaveBlockerServerLevel {
    @Shadow @Nonnull public abstract MinecraftServer getServer();

    @Inject(
        method = "save",
        at = @At("HEAD"),
        cancellable = true
    )
    private void save(ProgressListener pProgress, boolean pFlush, boolean pSkipSave, CallbackInfo ci) {
        if (((BackupState) getServer()).isBackingUp()) {
            TotallySafeBackup.LOGGER.info("Blocked save attempt during backup!");
            ci.cancel();
        }
    }
}
