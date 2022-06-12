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
import net.octyl.totallysafebackup.TotallySafeBackup;
import net.octyl.totallysafebackup.backup.BackupState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class SaveBlockerMinecraftServer {
    // Deny saving if we're backing up
    @Inject(
        method = "saveEverything",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSaveEverything(boolean p_195515_, boolean p_195516_, boolean p_195517_, CallbackInfoReturnable<Boolean> cir) {
        if (((BackupState) this).isBackingUp()) {
            TotallySafeBackup.LOGGER.info("Blocked save attempt during backup!");
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "saveAllChunks",
        at = @At("HEAD"),
        cancellable = true
    )
    public void saveAllChunks(boolean pSuppressLog, boolean pFlush, boolean pForced, CallbackInfoReturnable<Boolean> cir) {
        if (((BackupState) this).isBackingUp()) {
            TotallySafeBackup.LOGGER.info("Blocked save attempt during backup!");
            cir.setReturnValue(false);
        }
    }
}
