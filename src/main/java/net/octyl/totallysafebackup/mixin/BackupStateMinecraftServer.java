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
import net.minecraft.world.level.storage.LevelStorageSource;
import net.octyl.totallysafebackup.backup.BackupState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftServer.class)
public abstract class BackupStateMinecraftServer implements BackupState {
    @Shadow
    @Final
    protected LevelStorageSource.LevelStorageAccess storageSource;

    private volatile boolean isBackingUp = false;

    @Override
    public void setBackingUp(boolean backingUp) {
        this.isBackingUp = backingUp;
    }

    @Override
    public boolean isBackingUp() {
        return this.isBackingUp;
    }

    @Override
    public LevelStorageSource.LevelStorageAccess getStorageAccess() {
        return this.storageSource;
    }
}
