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

import org.apache.commons.io.function.IOConsumer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * A place to store backups. Should be closed to save the file to disk.
 */
public interface BackupTarget {
    /**
     * Save the given file, using {@code writer} to write the contents.
     *
     * <p>
     * The path must be relative.
     * </p>
     *
     * @param path the path to save
     * @param writer the writer to use
     * @throws IOException if there is an error saving the file
     */
    void saveFile(Path path, IOConsumer<OutputStream> writer) throws IOException;

    /**
     * Commit all saved files, and refuse any more.
     *
     * @throws IOException if there is an error committing the files
     */
    void commit() throws IOException;
}
