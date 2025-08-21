```java
package org.opentripplanner.datastore.file;

import org.opentripplanner.datastore.CompositeDataSource;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * This is a wrapper around a ZipFile, it can be used to read the content, but
 * not write to it. The {@link #asOutputStream()} is throwing an exception.
 */
public class ZipFileDataSource extends AbstractFileDataSource implements CompositeDataSource {
    private static final Logger LOG = LoggerFactory.getLogger(ZipFileDataSource.class);

    private boolean contentLoaded = false;
    private ZipFile zipFile;
    private final Collection<DataSource> content = new ArrayList<>();

    // Thresholds to prevent Zip Bomb attacks
    private static final int THRESHOLD_ENTRIES = 10000;
    private static final long THRESHOLD_SIZE = 1000000000L; // 1 GB
    private static final double THRESHOLD_RATIO = 10.0;

    public ZipFileDataSource(File file, FileType type) {
        super(file, type);
    }

    @Override
    public void close() {
        try {
            if(zipFile != null) {
                zipFile.close();
                zipFile = null;
            }
        }
        catch (IOException e) {
            LOG.error(path() + " close failed. Details: " + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public Collection<DataSource> content() {
        loadContent();
        return content;
    }

    @Override
    public DataSource entry(String name) {
        loadContent();
        return content.stream()
                .filter(it -> it.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * @return the internal zip file if still open. {@code null} is return if the file is closed.
     */
    ZipFile zipFile() {
        return zipFile;
    }

    private void loadContent() {
        // Load content once
        if(contentLoaded) { return; }
        contentLoaded = true;

        try {
            // The get name on ZipFile returns the full path, we want just the name.
            this.zipFile = new ZipFile(file, ZipFile.OPEN_READ);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            int totalEntries = 0;
            long totalUncompressedSize = 0L;

            while (entries.hasMoreElements()) {
                if(totalEntries >= THRESHOLD_ENTRIES) {
                    LOG.error("Zip archive {} has over {} entries, aborting to avoid Zip Bomb attack", path(), THRESHOLD_ENTRIES);
                    break;
                }

                ZipEntry entry = entries.nextElement();

                // We do not trust the entry sizes from the header; extract size by reading entry to count bytes
                long entryUncompressedSize = 0L;
                long entryCompressedSize = entry.getCompressedSize() > 0 ? entry.getCompressedSize() : 1; // avoid division by zero
                try (var is = zipFile.getInputStream(entry)) {
                    byte[] buffer = new byte[2048];
                    int readCount;
                    while ((readCount = is.read(buffer)) > 0) {
                        entryUncompressedSize += readCount;

                        if ((double)entryUncompressedSize / entryCompressedSize > THRESHOLD_RATIO) {
                            LOG.error("Entry {} in zip archive {} has suspicious compression ratio {}, aborting to avoid Zip Bomb attack",
                                    entry.getName(), path(), 
                                    ((double)entryUncompressedSize / entryCompressedSize));
                            entryUncompressedSize = -1; // Mark invalid size to skip entry
                            break;
                        }

                        long potentialTotalSize = totalUncompressedSize + entryUncompressedSize;
                        if (potentialTotalSize > THRESHOLD_SIZE) {
                            LOG.error("Zip archive {} uncompressed data exceeds {} bytes, aborting to avoid Zip Bomb attack",
                                    path(), THRESHOLD_SIZE);
                            entryUncompressedSize = -1; // Mark invalid size to skip entry
                            break;
                        }
                    }
                } catch (IOException e) {
                    LOG.error("Error reading entry {} in zip archive {}: {}", entry.getName(), path(), e.getMessage());
                    continue;
                }

                if (entryUncompressedSize == -1) {
                    // Skip adding this suspicious entry to content
                    break; // or continue — defensive to break to stop further processing
                }

                totalEntries++;
                totalUncompressedSize += entryUncompressedSize;

                content.add(new ZipFileEntryDataSource(this, entry));
            }
        }
        catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load " + path() + ": " + e.getLocalizedMessage(),
                    e
            );
        }
    }
}

