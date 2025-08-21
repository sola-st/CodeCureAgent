```java
package org.opentripplanner.datastore.file;

import org.opentripplanner.datastore.CompositeDataSource;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    // Threshold constants to limit resource usage and avoid zip bombs
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

            int totalEntryCount = 0;
            long totalUncompressedSize = 0;

            while (entries.hasMoreElements()) {
                if (totalEntryCount >= THRESHOLD_ENTRIES) {
                    LOG.error("Aborting zip extraction: too many entries in archive (>{})", THRESHOLD_ENTRIES);
                    break;
                }

                ZipEntry entry = entries.nextElement();

                // We open the input stream and count the real uncompressed data size
                long entryUncompressedSize = 0;
                long entryCompressedSize = entry.getCompressedSize();

                if (entryCompressedSize <= 0) {
                    // Defensive: if compressed size unknown or zero, set to 1 to avoid division by zero
                    entryCompressedSize = 1;
                }

                try (InputStream is = new BufferedInputStream(zipFile.getInputStream(entry))) {
                    byte[] buffer = new byte[2048];
                    int read;
                    while ((read = is.read(buffer)) > 0) {
                        entryUncompressedSize += read;
                        totalUncompressedSize += read;

                        double ratio = (double)entryUncompressedSize / (double)entryCompressedSize;
                        if (ratio > THRESHOLD_RATIO) {
                            LOG.error("Aborting zip extraction: compression ratio too high for entry '{}'. Possible zip bomb.", entry.getName());
                            break;
                        }

                        if (totalUncompressedSize > THRESHOLD_SIZE) {
                            LOG.error("Aborting zip extraction: total uncompressed size too large (>{} bytes). Possible zip bomb.", THRESHOLD_SIZE);
                            break;
                        }
                    }
                } catch (IOException e) {
                    LOG.error("Failed to read zip entry '{}': {}", entry.getName(), e.getMessage());
                    continue; // skip this entry on error
                }

                // If thresholds were exceeded inside reading, stop processing entries
                if (totalUncompressedSize > THRESHOLD_SIZE || totalEntryCount >= THRESHOLD_ENTRIES) {
                    break;
                }

                content.add(new ZipFileEntryDataSource(this, entry));
                totalEntryCount++;
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

