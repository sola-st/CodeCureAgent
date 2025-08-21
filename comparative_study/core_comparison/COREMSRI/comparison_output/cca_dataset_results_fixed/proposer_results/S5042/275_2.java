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

    // Threshold constants to mitigate zip bomb risks
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

            int totalEntryArchive = 0;
            long totalSizeArchive = 0;

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                // Prevent directories being counted as entries
                if (entry.isDirectory()) {
                    continue;
                }

                totalEntryArchive++;
                if (totalEntryArchive > THRESHOLD_ENTRIES) {
                    throw new RuntimeException("Too many entries in the archive: " + totalEntryArchive);
                }

                // Calculate actual uncompressed size of entry by reading its data
                long totalSizeEntry = 0;
                try (InputStream in = new BufferedInputStream(zipFile.getInputStream(entry))) {
                    byte[] buffer = new byte[2048];
                    int nBytes;
                    while ((nBytes = in.read(buffer)) > 0) {
                        totalSizeEntry += nBytes;

                        // Check compression ratio
                        long compressedSize = entry.getCompressedSize();
                        // compressedSize can be -1 if unknown, so skip ratio check if so
                        if (compressedSize > 0) {
                            double compressionRatio = (double) totalSizeEntry / compressedSize;
                            if (compressionRatio > THRESHOLD_RATIO) {
                                throw new RuntimeException("Compression ratio too high for entry: " + entry.getName());
                            }
                        }

                        totalSizeArchive += nBytes;
                        if (totalSizeArchive > THRESHOLD_SIZE) {
                            throw new RuntimeException("Total uncompressed size of archive exceeds threshold");
                        }
                    }
                }

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
