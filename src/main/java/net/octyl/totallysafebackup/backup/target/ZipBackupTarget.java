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

package net.octyl.totallysafebackup.backup.target;

import com.google.common.base.Preconditions;
import org.apache.commons.io.function.IOConsumer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Simple {@link ZipOutputStream}-based {@link BackupTarget}.
 */
public class ZipBackupTarget implements BackupTarget {
    private final String description;
    private final ZipOutputStream zip;

    /**
     * Create a new {@link ZipBackupTarget}.
     *
     * @param description the description of the backup target
     * @param zip the zip output stream to write to
     */
    public ZipBackupTarget(String description, ZipOutputStream zip) {
        this.description = description;
        this.zip = zip;
    }

    @Override
    public void saveFile(Path path, IOConsumer<OutputStream> writer) throws IOException {
        Preconditions.checkArgument(!path.isAbsolute(), "Path must be relative");
        zip.putNextEntry(new ZipEntry(path.toString()));
        writer.accept(zip);
        zip.closeEntry();
    }

    @Override
    public void commit() throws IOException {
        zip.close();
    }

    @Override
    public String toString() {
        return "ZipBackupTarget[" + description + "]";
    }
}
