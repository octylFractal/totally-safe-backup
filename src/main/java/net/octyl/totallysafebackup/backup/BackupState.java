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

import net.minecraft.world.level.storage.LevelStorageSource;

/**
 * Backup state for a server.
 */
public interface BackupState {
    /**
     * Set whether the server is backing up.
     *
     * @param backingUp whether the server is backing up
     */
    void setBackingUp(boolean backingUp);

    /**
     * {@return whether the server is backing up}
     */
    boolean isBackingUp();

    /**
     * {@return the level storage access}
     */
    LevelStorageSource.LevelStorageAccess getStorageAccess();
}
