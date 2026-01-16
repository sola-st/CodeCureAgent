class HStore{
    /**
     * Write an update
     * @param key
     * @param value
     */
    void add(final HStoreKey key, final byte[] value) {
        this.mc_lock.readLock().lock();
        try {
            mc.put(key, value);
        } finally {
            this.mc_lock.readLock().unlock();
        }
    }

    /** {@inheritDoc} */
    public void close() {
        if (!scannerClosed) {
            scannerClosed = true;
        }
    }

    /**
     * Prior to doing a cache flush, we need to snapshot the memcache.
     * TODO: This method is ugly.  Why let client of HStore run snapshots.  How
     * do we know they'll be cleaned up?
     */
    void snapshotMemcache() {
        this.memcache.snapshot();
    }

    /**
     * Write out current snapshot.  Presumes {@link #snapshot()} has been called
     * previously.
     * @param logCacheFlushId flush sequence number
     * @return count of bytes flushed
     * @throws IOException
     */
    long flushCache(final long logCacheFlushId) throws IOException {
        // Get the snapshot to flush.  Presumes that a call to
        // this.memcache.snapshot() has happened earlier up in the chain.
        SortedMap<HStoreKey, byte []> cache = this.memcache.getSnapshot();
        long flushed = internalFlushCache(cache, logCacheFlushId);
        // If an exception happens flushing, we let it out without clearing
        // the memcache snapshot.  The old snapshot will be returned when we say
        // 'snapshot', the next time flush comes around.
        this.memcache.clearSnapshot(cache);
        return flushed;
    }

    /*
     * Notify all observers that set of Readers has changed.
     * @throws IOException
     */
    private void notifyChangedReadersObservers() throws IOException {
        synchronized (this.changedReaderObservers) {
            for (ChangedReadersObserver o: this.changedReaderObservers) {
                o.updateReaders();
            }
        }
    }

    /*
     * @param o Observer who wants to know about changes in set of Readers
     */
    void addChangedReaderObserver(ChangedReadersObserver o) {
        this.changedReaderObservers.add(o);
    }

    /*
     * @param o Observer no longer interested in changes in set of Readers.
     */
    void deleteChangedReaderObserver(ChangedReadersObserver o) {
        if (!this.changedReaderObservers.remove(o)) {
            LOG.warn("Not in set" + o);
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    // Compaction
    //////////////////////////////////////////////////////////////////////////////

    /**
     * @return True if this store needs compaction.
     */
    boolean needsCompaction() {
        return this.storefiles != null &&
                (this.storefiles.size() >= this.compactionThreshold || hasReferences());
    }

    boolean compact(final boolean force) throws IOException {
        synchronized (compactLock) {
            // Storefiles are keyed by sequence id. The oldest file comes first.
            // We need to return out of here a List that has the newest file first.
            List<HStoreFile> filesToCompact =
                    new ArrayList<HStoreFile>(this.storefiles.values());
            if (filesToCompact.size() == 0) {
                return false;
            }
            if (!force && !hasReferences(filesToCompact) &&
                    filesToCompact.size() < compactionThreshold) {
                return false;
            }
            Collections.reverse(filesToCompact);
            if (!fs.exists(compactionDir) && !fs.mkdirs(compactionDir)) {
                LOG.warn("Mkdir on " + compactionDir.toString() + " failed");
                return false;
            }

            // Step through them, writing to the brand-new MapFile
            HStoreFile compactedOutputFile = new HStoreFile(conf, fs,
                    this.compactionDir, info.getEncodedName(), family.getFamilyName(),
                    -1L, null);
            if (LOG.isDebugEnabled()) {
                LOG.debug("started compaction of " + filesToCompact.size() +
                        " files " + filesToCompact.toString() + " into " +
                        FSUtils.getPath(compactedOutputFile.getMapFilePath()));
            }
            MapFile.Writer compactedOut = compactedOutputFile.getWriter(this.fs,
                    this.compression, this.bloomFilter);
            try {
                compactHStoreFiles(compactedOut, filesToCompact);
            } finally {
                compactedOut.close();
            }

            // Now, write out an HSTORE_LOGINFOFILE for the brand-new TreeMap.
            // Compute max-sequenceID seen in any of the to-be-compacted TreeMaps.
            long maxId = getMaxSequenceId(filesToCompact);
            compactedOutputFile.writeInfo(fs, maxId);

            // Move the compaction into place.
            completeCompaction(filesToCompact, compactedOutputFile);
            return true;
        }
    }

    /*
     * It's assumed that the compactLock  will be acquired prior to calling this
     * method!  Otherwise, it is not thread-safe!
     *
     * It works by processing a compaction that's been written to disk.
     *
     * <p>It is usually invoked at the end of a compaction, but might also be
     * invoked at HStore startup, if the prior execution died midway through.
     *
     * <p>Moving the compacted TreeMap into place means:
     * <pre>
     * 1) Moving the new compacted MapFile into place
     * 2) Unload all replaced MapFiles, close and collect list to delete.
     * 3) Loading the new TreeMap.
     * </pre>
     *
     * @param compactedFiles list of files that were compacted
     * @param compactedFile HStoreFile that is the result of the compaction
     * @throws IOException
     */
    private void completeCompaction(final List<HStoreFile> compactedFiles,
                                    final HStoreFile compactedFile)
            throws IOException {
        this.lock.writeLock().lock();
        try {
            // 1. Moving the new MapFile into place.
            HStoreFile finalCompactedFile = new HStoreFile(conf, fs, basedir,
                    info.getEncodedName(), family.getFamilyName(), -1, null);
            if (LOG.isDebugEnabled()) {
                LOG.debug("moving " + FSUtils.getPath(compactedFile.getMapFilePath()) +
                        " to " + FSUtils.getPath(finalCompactedFile.getMapFilePath()));
            }
            if (!compactedFile.rename(this.fs, finalCompactedFile)) {
                LOG.error("Failed move of compacted file " +
                        finalCompactedFile.getMapFilePath().toString());
                return;
            }

            // 2. Unload all replaced MapFiles, close and collect list to delete.
            synchronized (storefiles) {
                Map<Long, HStoreFile> toDelete = new HashMap<Long, HStoreFile>();
                for (Map.Entry<Long, HStoreFile> e : this.storefiles.entrySet()) {
                    if (!compactedFiles.contains(e.getValue())) {
                        continue;
                    }
                    Long key = e.getKey();
                    MapFile.Reader reader = this.readers.remove(key);
                    if (reader != null) {
                        reader.close();
                    }
                    toDelete.put(key, e.getValue());
                }

                try {
                    // 3. Loading the new TreeMap.
                    // Change this.storefiles so it reflects new state but do not
                    // delete old store files until we have sent out notification of
                    // change in case old files are still being accessed by outstanding
                    // scanners.
                    for (Long key : toDelete.keySet()) {
                        this.storefiles.remove(key);
                    }
                    // Add new compacted Reader and store file.
                    Long orderVal = Long.valueOf(finalCompactedFile.loadInfo(fs));
                    this.readers.put(orderVal,
                            // Use a block cache (if configured) for this reader since
                            // it is the only one.
                            finalCompactedFile.getReader(this.fs, this.bloomFilter));
                    this.storefiles.put(orderVal, finalCompactedFile);
                    // Tell observers that list of Readers has changed.
                    notifyChangedReadersObservers();
                    // Finally, delete old store files.
                    for (HStoreFile hsf : toDelete.values()) {
                        hsf.delete();
                    }
                } catch (IOException e) {
                    e = RemoteExceptionHandler.checkIOException(e);
                    LOG.error("Failed replacing compacted files for " + this.storeName +
                            ". Compacted file is " + finalCompactedFile.toString() +
                            ".  Files replaced are " + compactedFiles.toString() +
                            " some of which may have been already removed", e);
                }
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    // Accessors.
    // (This is the only section that is directly useful!)
    //////////////////////////////////////////////////////////////////////////////

    /**
     * Return all the available columns for the given key.  The key indicates a
     * row and timestamp, but not a column name.
     *
     * The returned object should map column names to byte arrays (byte[]).
     */
    void getFull(HStoreKey key, TreeMap<Text, byte []> results)
            throws IOException {
        Map<Text, Long> deletes = new HashMap<Text, Long>();

        if (key == null) {
            return;
        }

        this.lock.readLock().lock();
        memcache.getFull(key, deletes, results);
        try {
            MapFile.Reader[] maparray = getReaders();
            for (int i = maparray.length - 1; i >= 0; i--) {
                MapFile.Reader map = maparray[i];
                getFullFromMapFile(map, key, deletes, results);
            }
        } finally {
            this.lock.readLock().unlock();
        }
    }

    MapFile.Reader [] getReaders() {
        return this.readers.values().
                toArray(new MapFile.Reader[this.readers.size()]);
    }

    /**
     * Get the value for the indicated HStoreKey.  Grab the target value and the
     * previous 'numVersions-1' values, as well.
     *
     * If 'numVersions' is negative, the method returns all available versions.
     * @param key
     * @param numVersions Number of versions to fetch.  Must be > 0.
     * @return values for the specified versions
     * @throws IOException
     */
    byte [][] get(HStoreKey key, int numVersions) throws IOException {
        if (numVersions <= 0) {
            throw new IllegalArgumentException("Number of versions must be > 0");
        }

        this.lock.readLock().lock();
        try {
            // Check the memcache
            List<byte[]> results = this.memcache.get(key, numVersions);
            // If we got sufficient versions from memcache, return.
            if (results.size() == numVersions) {
                return ImmutableBytesWritable.toArray(results);
            }

            // Keep a list of deleted cell keys.  We need this because as we go through
            // the store files, the cell with the delete marker may be in one file and
            // the old non-delete cell value in a later store file. If we don't keep
            // around the fact that the cell was deleted in a newer record, we end up
            // returning the old value if user is asking for more than one version.
            // This List of deletes should not large since we are only keeping rows
            // and columns that match those set on the scanner and which have delete
            // values.  If memory usage becomes an issue, could redo as bloom filter.
            Map<Text, List<Long>> deletes = new HashMap<Text, List<Long>>();
            // This code below is very close to the body of the getKeys method.
            MapFile.Reader[] maparray = getReaders();
            for(int i = maparray.length - 1; i >= 0; i--) {
                MapFile.Reader map = maparray[i];
                synchronized(map) {
                    map.reset();
                    ImmutableBytesWritable readval = new ImmutableBytesWritable();
                    HStoreKey readkey = (HStoreKey)map.getClosest(key, readval);
                    if (readkey == null) {
                        // map.getClosest returns null if the passed key is > than the
                        // last key in the map file.  getClosest is a bit of a misnomer
                        // since it returns exact match or the next closest key AFTER not
                        // BEFORE.
                        continue;
                    }
                    if (!readkey.matchesRowCol(key)) {
                        continue;
                    }
                    if (!isDeleted(readkey, readval.get(), true, deletes)) {
                        results.add(readval.get());
                        // Perhaps only one version is wanted.  I could let this
                        // test happen later in the for loop test but it would cost
                        // the allocation of an ImmutableBytesWritable.
                        if (hasEnoughVersions(numVersions, results)) {
                            break;
                        }
                    }
                    for (readval = new ImmutableBytesWritable();
                         map.next(readkey, readval) &&
                                 readkey.matchesRowCol(key) &&
                                 !hasEnoughVersions(numVersions, results);
                         readval = new ImmutableBytesWritable()) {
                        if (!isDeleted(readkey, readval.get(), true, deletes)) {
                            results.add(readval.get());
                        }
                    }
                }
                if (hasEnoughVersions(numVersions, results)) {
                    break;
                }
            }
            return results.size() == 0 ?
                    null : ImmutableBytesWritable.toArray(results);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /**
     * Get <code>versions</code> keys matching the origin key's
     * row/column/timestamp and those of an older vintage
     * Default access so can be accessed out of {@link HRegionServer}.
     * @param origin Where to start searching.
     * @param versions How many versions to return. Pass
     * {@link HConstants.ALL_VERSIONS} to retrieve all. Versions will include
     * size of passed <code>allKeys</code> in its count.
     * @param allKeys List of keys prepopulated by keys we found in memcache.
     * This method returns this passed list with all matching keys found in
     * stores appended.
     * @return The passed <code>allKeys</code> with <code>versions</code> of
     * matching keys found in store files appended.
     * @throws IOException
     */
    List<HStoreKey> getKeys(final HStoreKey origin, final int versions)
            throws IOException {

        List<HStoreKey> keys = this.memcache.getKeys(origin, versions);
        if (versions != ALL_VERSIONS && keys.size() >= versions) {
            return keys;
        }

        // This code below is very close to the body of the get method.
        this.lock.readLock().lock();
        try {
            MapFile.Reader[] maparray = getReaders();
            for(int i = maparray.length - 1; i >= 0; i--) {
                MapFile.Reader map = maparray[i];
                synchronized(map) {
                    map.reset();

                    // do the priming read
                    ImmutableBytesWritable readval = new ImmutableBytesWritable();
                    HStoreKey readkey = (HStoreKey)map.getClosest(origin, readval);
                    if (readkey == null) {
                        // map.getClosest returns null if the passed key is > than the
                        // last key in the map file.  getClosest is a bit of a misnomer
                        // since it returns exact match or the next closest key AFTER not
                        // BEFORE.
                        continue;
                    }

                    do{
                        // if the row matches, we might want this one.
                        if(rowMatches(origin, readkey)){
                            // if the cell matches, then we definitely want this key.
                            if (cellMatches(origin, readkey)) {
                                // store the key if it isn't deleted or superceeded by what's
                                // in the memcache
                                if (!isDeleted(readkey, readval.get(), false, null) &&
                                        !keys.contains(readkey)) {
                                    keys.add(new HStoreKey(readkey));

                                    // if we've collected enough versions, then exit the loop.
                                    if (versions != ALL_VERSIONS && keys.size() >= versions) {
                                        break;
                                    }
                                }
                            } else {
                                // the cell doesn't match, but there might be more with different
                                // timestamps, so move to the next key
                                continue;
                            }
                        } else{
                            // the row doesn't match, so we've gone too far.
                            break;
                        }
                    }while(map.next(readkey, readval)); // advance to the next key
                }
            }

            return keys;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /**
     * Find the key that matches <i>row</i> exactly, or the one that immediately
     * preceeds it. WARNING: Only use this method on a table where writes occur
     * with stricly increasing timestamps. This method assumes this pattern of
     * writes in order to make it reasonably performant.
     */
    Text getRowKeyAtOrBefore(final Text row)
            throws IOException{
        // map of HStoreKeys that are candidates for holding the row key that
        // most closely matches what we're looking for. we'll have to update it
        // deletes found all over the place as we go along before finally reading
        // the best key out of it at the end.
        SortedMap<HStoreKey, Long> candidateKeys = new TreeMap<HStoreKey, Long>();

        // obtain read lock
        this.lock.readLock().lock();
        try {
            MapFile.Reader[] maparray = getReaders();

            // process each store file
            for(int i = maparray.length - 1; i >= 0; i--) {
                // update the candidate keys from the current map file
                rowAtOrBeforeFromMapFile(maparray[i], row, candidateKeys);
            }

            // finally, check the memcache
            this.memcache.getRowKeyAtOrBefore(row, candidateKeys);

            // return the best key from candidateKeys
            if (!candidateKeys.isEmpty()) {
                return candidateKeys.lastKey().getRow();
            }
            return null;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /**
     * Gets size for the store.
     *
     * @param midKey Gets set to the middle key of the largest splitable store
     * file or its set to empty if largest is not splitable.
     * @return Sizes for the store and the passed <code>midKey</code> is
     * set to midKey of largest splitable.  Otherwise, its set to empty
     * to indicate we couldn't find a midkey to split on
     */
    HStoreSize size(Text midKey) {
        long maxSize = 0L;
        long aggregateSize = 0L;
        // Not splitable if we find a reference store file present in the store.
        boolean splitable = true;
        if (this.storefiles.size() <= 0) {
            return new HStoreSize(0, 0, splitable);
        }

        this.lock.readLock().lock();
        try {
            Long mapIndex = Long.valueOf(0L);
            // Iterate through all the MapFiles
            for (Map.Entry<Long, HStoreFile> e: storefiles.entrySet()) {
                HStoreFile curHSF = e.getValue();
                long size = curHSF.length();
                aggregateSize += size;
                if (maxSize == 0L || size > maxSize) {
                    // This is the largest one so far
                    maxSize = size;
                    mapIndex = e.getKey();
                }
                if (splitable) {
                    splitable = !curHSF.isReference();
                }
            }
            if (splitable) {
                MapFile.Reader r = this.readers.get(mapIndex);
                // seek back to the beginning of mapfile
                r.reset();
                // get the first and last keys
                HStoreKey firstKey = new HStoreKey();
                HStoreKey lastKey = new HStoreKey();
                Writable value = new ImmutableBytesWritable();
                r.next(firstKey, value);
                r.finalKey(lastKey);
                // get the midkey
                HStoreKey mk = (HStoreKey)r.midKey();
                if (mk != null) {
                    // if the midkey is the same as the first and last keys, then we cannot
                    // (ever) split this region.
                    if (mk.getRow().equals(firstKey.getRow()) &&
                            mk.getRow().equals(lastKey.getRow())) {
                        return new HStoreSize(aggregateSize, maxSize, false);
                    }
                    // Otherwise, set midKey
                    midKey.set(mk.getRow());
                }
            }
        } catch(IOException e) {
            LOG.warn("Failed getting store size for " + this.storeName, e);
        } finally {
            this.lock.readLock().unlock();
        }
        return new HStoreSize(aggregateSize, maxSize, splitable);
    }

    /**
     * Return a scanner for both the memcache and the HStore files
     */
    HInternalScannerInterface getScanner(long timestamp, Text targetCols[],
                                         Text firstRow, RowFilterInterface filter)
            throws IOException {
        lock.readLock().lock();
        try {
            return new HStoreScanner(targetCols, firstRow, timestamp, filter);
        } finally {
            lock.readLock().unlock();
        }
    }
    /** {@inheritDoc} */
    public String toString() {
        return this.storeName;
    }

    /**
     * @return Current list of store files.
     */
    SortedMap<Long, HStoreFile> getStorefiles() {
        synchronized (this.storefiles) {
            SortedMap<Long, HStoreFile> copy =
                    new TreeMap<Long, HStoreFile>(this.storefiles);
            return copy;
        }
    }
}
class MemcacheHandler{
    /*
     * Utility method.
     * @return sycnhronized sorted map of HStoreKey to byte arrays.
     */
    private static SortedMap<HStoreKey, byte[]> createSynchronizedSortedMap() {
        return Collections.synchronizedSortedMap(new TreeMap<HStoreKey, byte []>());
    }

    void snapshot() {
        this.mc_lock.writeLock().lock();
        try {
            // If snapshot currently has entries, then flusher failed or didn't call
            // cleanup.  Log a warning.
            if (this.snapshot.size() > 0) {
                LOG.debug("Snapshot called again without clearing previous. " +
                        "Doing nothing. Another ongoing flush or did we fail last attempt?");
            } else {
                // We used to synchronize on the memcache here but we're inside a
                // write lock so removed it. Comment is left in case removal was a
                // mistake. St.Ack
                if (this.mc.size() != 0) {
                    this.snapshot = this.mc;
                    this.mc = createSynchronizedSortedMap();
                }
            }
        } finally {
            this.mc_lock.writeLock().unlock();
        }
    }

    /**
     * Return the current snapshot.
     * Called by flusher when it wants to clean up snapshot made by a previous
     * call to {@link snapshot}
     * @return Return snapshot.
     * @see {@link #snapshot()}
     * @see {@link #clearSnapshot(SortedMap)}
     */
    SortedMap<HStoreKey, byte[]> getSnapshot() {
        return this.snapshot;
    }

    /**
     * The passed snapshot was successfully persisted; it can be let go.
     * @param ss The snapshot to clean out.
     * @throws UnexpectedException
     * @see {@link #snapshot()}
     */
    void clearSnapshot(final SortedMap<HStoreKey, byte []> ss)
            throws UnexpectedException {
        this.mc_lock.writeLock().lock();
        try {
            if (this.snapshot != ss) {
                throw new UnexpectedException("Current snapshot is " +
                        this.snapshot + ", was passed " + ss);
            }
            // OK. Passed in snapshot is same as current snapshot.  If not-empty,
            // create a new snapshot and let the old one go.
            if (ss.size() != 0) {
                this.snapshot = createSynchronizedSortedMap();
            }
        } finally {
            this.mc_lock.writeLock().unlock();
        }
    }

    /**
     * Write an update
     * @param key
     * @param value
     */
    void add(final HStoreKey key, final byte[] value) {
        this.mc_lock.readLock().lock();
        try {
            mc.put(key, value);
        } finally {
            this.mc_lock.readLock().unlock();
        }
    }

    /**
     * Look back through all the backlog TreeMaps to find the target.
     * @param key
     * @param numVersions
     * @return An array of byte arrays ordered by timestamp.
     */
    List<byte[]> get(final HStoreKey key, final int numVersions) {
        this.mc_lock.readLock().lock();
        try {
            List<byte []> results;
            // The synchronizations here are because internalGet iterates
            synchronized (this.mc) {
                results = internalGet(this.mc, key, numVersions);
            }
            synchronized (snapshot) {
                results.addAll(results.size(),
                        internalGet(snapshot, key, numVersions - results.size()));
            }
            return results;
        } finally {
            this.mc_lock.readLock().unlock();
        }
    }

    /**
     * Return all the available columns for the given key.  The key indicates a
     * row and timestamp, but not a column name.
     *
     * The returned object should map column names to byte arrays (byte[]).
     * @param key
     * @param results
     * @return most recent timestamp found
     */
    long getFull(HStoreKey key, Map<Text, Long> deletes,
                 SortedMap<Text, byte[]> results) {
        long rowtime = -1L;

        this.mc_lock.readLock().lock();
        try {
            synchronized (mc) {
                long ts = internalGetFull(mc, key, deletes, results);
                if (ts != HConstants.LATEST_TIMESTAMP && ts > rowtime) {
                    rowtime = ts;
                }
            }
            synchronized (snapshot) {
                long ts = internalGetFull(snapshot, key, deletes, results);
                if (ts != HConstants.LATEST_TIMESTAMP && ts > rowtime) {
                    rowtime = ts;
                }
            }
            return rowtime;
        } finally {
            this.mc_lock.readLock().unlock();
        }
    }

    /**
     * Find the key that matches <i>row</i> exactly, or the one that immediately
     * preceeds it.
     * @param row Row to look for.
     * @param candidateKeys Map of candidate keys (Accumulation over lots of
     * lookup over stores and memcaches)
     */
    void getRowKeyAtOrBefore(final Text row,
                             SortedMap<HStoreKey, Long> candidateKeys) {
        this.mc_lock.readLock().lock();

        try {
            synchronized (mc) {
                internalGetRowKeyAtOrBefore(mc, row, candidateKeys);
            }
            synchronized (snapshot) {
                internalGetRowKeyAtOrBefore(snapshot, row, candidateKeys);
            }
        } finally {
            this.mc_lock.readLock().unlock();
        }
    }
}
class FileAdministration{

    /*
     * Read the reconstructionLog to see whether we need to build a brand-new
     * MapFile out of non-flushed log entries.
     *
     * We can ignore any log message that has a sequence ID that's equal to or
     * lower than maxSeqID.  (Because we know such log messages are already
     * reflected in the MapFiles.)
     */
    private void doReconstructionLog(final Path reconstructionLog,
                                     final long maxSeqID, final Progressable reporter)
            throws UnsupportedEncodingException, IOException {

        if (reconstructionLog == null || !fs.exists(reconstructionLog)) {
            // Nothing to do.
            return;
        }
        // Check its not empty.
        FileStatus[] stats = fs.listStatus(reconstructionLog);
        if (stats == null || stats.length == 0) {
            LOG.warn("Passed reconstruction log " + reconstructionLog + " is zero-length");
            return;
        }
        long maxSeqIdInLog = -1;
        TreeMap<HStoreKey, byte []> reconstructedCache =
                new TreeMap<HStoreKey, byte []>();

        SequenceFile.Reader logReader = new SequenceFile.Reader(this.fs,
                reconstructionLog, this.conf);

        try {
            HLogKey key = new HLogKey();
            HLogEdit val = new HLogEdit();
            long skippedEdits = 0;
            long editsCount = 0;
            // How many edits to apply before we send a progress report.
            int reportInterval = this.conf.getInt("hbase.hstore.report.interval.edits", 2000);
            while (logReader.next(key, val)) {
                maxSeqIdInLog = Math.max(maxSeqIdInLog, key.getLogSeqNum());
                if (key.getLogSeqNum() <= maxSeqID) {
                    skippedEdits++;
                    continue;
                }
                // Check this edit is for me. Also, guard against writing
                // METACOLUMN info such as HBASE::CACHEFLUSH entries
                Text column = val.getColumn();
                if (column.equals(HLog.METACOLUMN)
                        || !key.getRegionName().equals(info.getRegionName())
                        || !HStoreKey.extractFamily(column).equals(family.getFamilyName())) {
                    continue;
                }
                HStoreKey k = new HStoreKey(key.getRow(), column, val.getTimestamp());
                reconstructedCache.put(k, val.getVal());
                editsCount++;
                // Every 2k edits, tell the reporter we're making progress.
                // Have seen 60k edits taking 3minutes to complete.
                if (reporter != null && (editsCount % reportInterval) == 0) {
                    reporter.progress();
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Applied " + editsCount + ", skipped " + skippedEdits +
                        " because sequence id <= " + maxSeqID);
            }
        } finally {
            logReader.close();
        }

        if (reconstructedCache.size() > 0) {
            // We create a "virtual flush" at maxSeqIdInLog+1.
            if (LOG.isDebugEnabled()) {
                LOG.debug("flushing reconstructionCache");
            }
            internalFlushCache(reconstructedCache, maxSeqIdInLog + 1);
        }
    }

    /*
     * Creates a series of HStoreFiles loaded from the given directory.
     * There must be a matching 'mapdir' and 'loginfo' pair of files.
     * If only one exists, we'll delete it.  Does other consistency tests
     * checking files are not zero, etc.
     *
     * @param infodir qualified path for info file directory
     * @param mapdir qualified path for map file directory
     * @throws IOException
     */
    private List<HStoreFile> loadHStoreFiles(Path infodir, Path mapdir)
            throws IOException {
        // Look first at info files.  If a reference, these contain info we need
        // to create the HStoreFile.
        Path infofiles[] = fs.listPaths(new Path[] {infodir});
        ArrayList<HStoreFile> results = new ArrayList<HStoreFile>(infofiles.length);
        ArrayList<Path> mapfiles = new ArrayList<Path>(infofiles.length);
        for (Path p: infofiles) {
            // Check for empty info file.  Should never be the case but can happen
            // after data loss in hdfs for whatever reason (upgrade, etc.): HBASE-646
            if (this.fs.getFileStatus(p).getLen() <= 0) {
                LOG.warn("Skipping " + p + " because its empty.  DATA LOSS?  Can " +
                        "this scenario be repaired?  HBASE-646");
                continue;
            }

            Matcher m = REF_NAME_PARSER.matcher(p.getName());
            /*
             *  *  *  *  *  N O T E  *  *  *  *  *
             *
             *  We call isReference(Path, Matcher) here because it calls
             *  Matcher.matches() which must be called before Matcher.group(int)
             *  and we don't want to call Matcher.matches() twice.
             *
             *  *  *  *  *  N O T E  *  *  *  *  *
             */
            boolean isReference = isReference(p, m);
            long fid = Long.parseLong(m.group(1));

            HStoreFile curfile = null;
            HStoreFile.Reference reference = null;
            if (isReference) {
                reference = readSplitInfo(p, fs);
            }
            curfile = new HStoreFile(conf, fs, basedir, info.getEncodedName(),
                    family.getFamilyName(), fid, reference);
            Path mapfile = curfile.getMapFilePath();
            if (!fs.exists(mapfile)) {
                fs.delete(curfile.getInfoFilePath());
                LOG.warn("Mapfile " + mapfile.toString() + " does not exist. Cleaned " +
                        "up info file.  Continuing...Probable DATA LOSS!!!");
                continue;
            }
            if (isEmptyDataFile(mapfile)) {
                curfile.delete();
                // We can have empty data file if data loss in hdfs.
                LOG.warn("Mapfile " + mapfile.toString() + " has empty data. " +
                        "Deleting.  Continuing...Probable DATA LOSS!!!  See HBASE-646.");
                continue;
            }
            if (isEmptyIndexFile(mapfile)) {
                try {
                    // Try fixing this file.. if we can.  Use the hbase version of fix.
                    // Need to remove the old index file first else fix won't go ahead.
                    this.fs.delete(new Path(mapfile, MapFile.INDEX_FILE_NAME));
                    long count = MapFile.fix(this.fs, mapfile, HbaseMapFile.KEY_CLASS,
                            HbaseMapFile.VALUE_CLASS, false, this.conf);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Fixed index on " + mapfile.toString() + "; had " +
                                count + " entries");
                    }
                } catch (Exception e) {
                    LOG.warn("Failed fix of " + mapfile.toString() +
                            "...continuing; Probable DATA LOSS!!!", e);
                    continue;
                }
            }

            // TODO: Confirm referent exists.

            // Found map and sympathetic info file.  Add this hstorefile to result.
            results.add(curfile);
            if (LOG.isDebugEnabled()) {
                LOG.debug("loaded " + FSUtils.getPath(p) + ", isReference=" +
                        isReference);
            }
            // Keep list of sympathetic data mapfiles for cleaning info dir in next
            // section.  Make sure path is fully qualified for compare.
            mapfiles.add(mapfile);
        }

        // List paths by experience returns fully qualified names -- at least when
        // running on a mini hdfs cluster.
        Path datfiles[] = fs.listPaths(new Path[] {mapdir});
        for (int i = 0; i < datfiles.length; i++) {
            // If does not have sympathetic info file, delete.
            if (!mapfiles.contains(fs.makeQualified(datfiles[i]))) {
                fs.delete(datfiles[i]);
            }
        }
        return results;
    }

    /*
     * @param mapfile
     * @return True if the passed mapfile has a zero-length data component (its
     * broken).
     * @throws IOException
     */
    private boolean isEmptyDataFile(final Path mapfile)
            throws IOException {
        // Mapfiles are made of 'data' and 'index' files.  Confirm 'data' is
        // non-null if it exists (may not have been written to yet).
        return isEmptyFile(new Path(mapfile, MapFile.DATA_FILE_NAME));
    }

    /*
     * @param mapfile
     * @return True if the passed mapfile has a zero-length index component (its
     * broken).
     * @throws IOException
     */
    private boolean isEmptyIndexFile(final Path mapfile)
            throws IOException {
        // Mapfiles are made of 'data' and 'index' files.  Confirm 'data' is
        // non-null if it exists (may not have been written to yet).
        return isEmptyFile(new Path(mapfile, MapFile.INDEX_FILE_NAME));
    }

    /*
     * @param mapfile
     * @return True if the passed mapfile has a zero-length index component (its
     * broken).
     * @throws IOException
     */
    private boolean isEmptyFile(final Path f)
            throws IOException {
        return this.fs.exists(f) &&
                this.fs.getFileStatus(f).getLen() == 0;
    }

    /*
     * @see writeSplitInfo(Path p, HStoreFile hsf, FileSystem fs)
     */
    static HStoreFile.Reference readSplitInfo(final Path p, final FileSystem fs)
            throws IOException {
        FSDataInputStream in = fs.open(p);
        try {
            HStoreFile.Reference r = new HStoreFile.Reference();
            r.readFields(in);
            return r;
        } finally {
            in.close();
        }
    }

    /**
     * @param p Path to check.
     * @return True if the path has format of a HStoreFile reference.
     */
    public static boolean isReference(final Path p) {
        return isReference(p, REF_NAME_PARSER.matcher(p.getName()));
    }

    private static boolean isReference(final Path p, final Matcher m) {
        if (m == null || !m.matches()) {
            LOG.warn("Failed match of store file name " + p.toString());
            throw new RuntimeException("Failed match of store file name " +
                    p.toString());
        }
        return m.groupCount() > 1 && m.group(2) != null;
    }
}
class CompactionHandler{
    /*
     * @return True if this store has references.
     */
    private boolean hasReferences() {
        return this.storefiles != null && this.storefiles.size() > 0 &&
                hasReferences(this.storefiles.values());
    }

    /*
     * @param files
     * @return True if any of the files in <code>files</code> are References.
     */
    private boolean hasReferences(Collection<HStoreFile> files) {
        if (files != null && files.size() > 0) {
            for (HStoreFile hsf: files) {
                if (hsf.isReference()) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * Compact passed <code>toCompactFiles</code> into <code>compactedOut</code>.
     * We create a new set of MapFile.Reader objects so we don't screw up the
     * caching associated with the currently-loaded ones. Our iteration-based
     * access pattern is practically designed to ruin the cache.
     *
     * We work by opening a single MapFile.Reader for each file, and iterating
     * through them in parallel. We always increment the lowest-ranked one.
     * Updates to a single row/column will appear ranked by timestamp. This allows
     * us to throw out deleted values or obsolete versions. @param compactedOut
     * @param toCompactFiles @throws IOException
     */
    private void compactHStoreFiles(final MapFile.Writer compactedOut,
                                    final List<HStoreFile> toCompactFiles) throws IOException {

        int size = toCompactFiles.size();
        CompactionReader[] rdrs = new CompactionReader[size];
        int index = 0;
        for (HStoreFile hsf: toCompactFiles) {
            try {
                rdrs[index++] =
                        new MapFileCompactionReader(hsf.getReader(fs, bloomFilter));
            } catch (IOException e) {
                // Add info about which file threw exception. It may not be in the
                // exception message so output a message here where we know the
                // culprit.
                LOG.warn("Failed with " + e.toString() + ": " + hsf.toString());
                closeCompactionReaders(rdrs);
                throw e;
            }
        }
        try {
            HStoreKey[] keys = new HStoreKey[rdrs.length];
            ImmutableBytesWritable[] vals = new ImmutableBytesWritable[rdrs.length];
            boolean[] done = new boolean[rdrs.length];
            for(int i = 0; i < rdrs.length; i++) {
                keys[i] = new HStoreKey();
                vals[i] = new ImmutableBytesWritable();
                done[i] = false;
            }

            // Now, advance through the readers in order.  This will have the
            // effect of a run-time sort of the entire dataset.
            int numDone = 0;
            for(int i = 0; i < rdrs.length; i++) {
                rdrs[i].reset();
                done[i] = ! rdrs[i].next(keys[i], vals[i]);
                if(done[i]) {
                    numDone++;
                }
            }

            int timesSeen = 0;
            Text lastRow = new Text();
            Text lastColumn = new Text();
            // Map of a row deletes keyed by column with a list of timestamps for value
            Map<Text, List<Long>> deletes = null;
            while (numDone < done.length) {
                // Find the reader with the smallest key.  If two files have same key
                // but different values -- i.e. one is delete and other is non-delete
                // value -- we will find the first, the one that was written later and
                // therefore the one whose value should make it out to the compacted
                // store file.
                int smallestKey = -1;
                for(int i = 0; i < rdrs.length; i++) {
                    if(done[i]) {
                        continue;
                    }
                    if(smallestKey < 0) {
                        smallestKey = i;
                    } else {
                        if(keys[i].compareTo(keys[smallestKey]) < 0) {
                            smallestKey = i;
                        }
                    }
                }

                // Reflect the current key/val in the output
                HStoreKey sk = keys[smallestKey];
                if(lastRow.equals(sk.getRow())
                        && lastColumn.equals(sk.getColumn())) {
                    timesSeen++;
                } else {
                    timesSeen = 1;
                    // We are on to a new row.  Create a new deletes list.
                    deletes = new HashMap<Text, List<Long>>();
                }

                byte [] value = (vals[smallestKey] == null)?
                        null: vals[smallestKey].get();
                if (!isDeleted(sk, value, false, deletes) &&
                        timesSeen <= family.getMaxVersions()) {
                    // Keep old versions until we have maxVersions worth.
                    // Then just skip them.
                    if (sk.getRow().getLength() != 0 && sk.getColumn().getLength() != 0) {
                        // Only write out objects which have a non-zero length key and
                        // value
                        compactedOut.append(sk, vals[smallestKey]);
                    }
                }

                // Update last-seen items
                lastRow.set(sk.getRow());
                lastColumn.set(sk.getColumn());

                // Advance the smallest key.  If that reader's all finished, then
                // mark it as done.
                if(!rdrs[smallestKey].next(keys[smallestKey],
                        vals[smallestKey])) {
                    done[smallestKey] = true;
                    rdrs[smallestKey].close();
                    rdrs[smallestKey] = null;
                    numDone++;
                }
            }
        } finally {
            closeCompactionReaders(rdrs);
        }
    }
    private void closeCompactionReaders(final CompactionReader [] rdrs) {
        for (int i = 0; i < rdrs.length; i++) {
            if (rdrs[i] != null) {
                try {
                    rdrs[i].close();
                } catch (IOException e) {
                    LOG.warn("Exception closing reader for " + this.storeName, e);
                }
            }
        }
    }
    /*
     * Check if this is cell is deleted.
     * If a memcache and a deletes, check key does not have an entry filled.
     * Otherwise, check value is not the <code>HGlobals.deleteBytes</code> value.
     * If passed value IS deleteBytes, then it is added to the passed
     * deletes map.
     * @param hsk
     * @param value
     * @param checkMemcache true if the memcache should be consulted
     * @param deletes Map keyed by column with a value of timestamp. Can be null.
     * If non-null and passed value is HGlobals.deleteBytes, then we add to this
     * map.
     * @return True if this is a deleted cell.  Adds the passed deletes map if
     * passed value is HGlobals.deleteBytes.
     */
    private boolean isDeleted(final HStoreKey hsk, final byte [] value,
                              final boolean checkMemcache, final Map<Text, List<Long>> deletes) {
        if (checkMemcache && memcache.isDeleted(hsk)) {
            return true;
        }
        List<Long> timestamps =
                (deletes == null) ? null: deletes.get(hsk.getColumn());
        if (timestamps != null &&
                timestamps.contains(Long.valueOf(hsk.getTimestamp()))) {
            return true;
        }
        if (value == null) {
            // If a null value, shouldn't be in here.  Mark it as deleted cell.
            return true;
        }
        if (!HLogEdit.isDeleted(value)) {
            return false;
        }
        // Cell has delete value.  Save it into deletes.
        if (deletes != null) {
            if (timestamps == null) {
                timestamps = new ArrayList<Long>();
                deletes.put(hsk.getColumn(), timestamps);
            }
            // We know its not already in the deletes array else we'd have returned
            // earlier so no need to test if timestamps already has this value.
            timestamps.add(Long.valueOf(hsk.getTimestamp()));
        }
        return true;
    }


}
class StoreFileScanner{
    private class StoreFileScanner extends HAbstractScanner
            implements ChangedReadersObserver {
        // Keys retrieved from the sources
        private HStoreKey keys[];

        // Values that correspond to those keys
        private byte [][] vals;

        private MapFile.Reader[] sfsReaders;

        // Used around replacement of Readers if they change while we're scanning.
        @SuppressWarnings("hiding")
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        StoreFileScanner(long timestamp, Text[] targetCols, Text firstRow)
                throws IOException {
            super(timestamp, targetCols);
            addChangedReaderObserver(this);
            try {
                openReaders(firstRow);
            } catch (Exception ex) {
                close();
                IOException e = new IOException("HStoreScanner failed construction");
                e.initCause(ex);
                throw e;
            }
        }

        /*
         * Go open new Reader iterators and cue them at <code>firstRow</code>.
         * Closes existing Readers if any.
         * @param firstRow
         * @throws IOException
         */
        private void openReaders(final Text firstRow) throws IOException {
            if (this.sfsReaders != null) {
                for (int i = 0; i < this.sfsReaders.length; i++) {
                    // Close readers that are not exhausted, set to null
                    if (this.sfsReaders[i] != null) {
                        this.sfsReaders[i].close();
                    }
                }
            }
            // Open our own copies of the Readers here inside in the scanner.
            this.sfsReaders = new MapFile.Reader[getStorefiles().size()];

            // Most recent map file should be first
            int i = sfsReaders.length - 1;
            for(HStoreFile curHSF: getStorefiles().values()) {
                sfsReaders[i--] = curHSF.getReader(fs, bloomFilter);
            }

            this.keys = new HStoreKey[sfsReaders.length];
            this.vals = new byte[sfsReaders.length][];

            // Advance the readers to the first pos.
            for (i = 0; i < sfsReaders.length; i++) {
                keys[i] = new HStoreKey();
                if (firstRow != null && firstRow.getLength() != 0) {
                    if (findFirstRow(i, firstRow)) {
                        continue;
                    }
                }
                while (getNext(i)) {
                    if (columnMatch(i)) {
                        break;
                    }
                }
            }
        }

        /**
         * For a particular column i, find all the matchers defined for the column.
         * Compare the column family and column key using the matchers. The first one
         * that matches returns true. If no matchers are successful, return false.
         *
         * @param i index into the keys array
         * @return true if any of the matchers for the column match the column family
         * and the column key.
         * @throws IOException
         */
        boolean columnMatch(int i) throws IOException {
            return columnMatch(keys[i].getColumn());
        }

        /**
         * @param key The key that matched
         * @param results All the results for <code>key</code>
         * @return true if a match was found
         * @throws IOException
         */
        @Override
        public boolean next(HStoreKey key, SortedMap<Text, byte []> results)
                throws IOException {
            if (scannerClosed) {
                return false;
            }
            this.lock.readLock().lock();
            try {
                // Find the next viable row label (and timestamp).
                ViableRow viableRow = getNextViableRow();

                // Grab all the values that match this row/timestamp
                boolean insertedItem = false;
                if (viableRow.getRow() != null) {
                    key.setRow(viableRow.getRow());
                    key.setVersion(viableRow.getTimestamp());
                    for (int i = 0; i < keys.length; i++) {
                        // Fetch the data
                        while ((keys[i] != null)
                                && (keys[i].getRow().compareTo(viableRow.getRow()) == 0)) {

                            // If we are doing a wild card match or there are multiple matchers
                            // per column, we need to scan all the older versions of this row
                            // to pick up the rest of the family members
                            if(!isWildcardScanner()
                                    && !isMultipleMatchScanner()
                                    && (keys[i].getTimestamp() != viableRow.getTimestamp())) {
                                break;
                            }
                            if(columnMatch(i)) {
                                // We only want the first result for any specific family member
                                if(!results.containsKey(keys[i].getColumn())) {
                                    results.put(new Text(keys[i].getColumn()), vals[i]);
                                    insertedItem = true;
                                }
                            }

                            if (!getNext(i)) {
                                closeSubScanner(i);
                            }
                        }
                        // Advance the current scanner beyond the chosen row, to
                        // a valid timestamp, so we're ready next time.
                        while ((keys[i] != null)
                                && ((keys[i].getRow().compareTo(viableRow.getRow()) <= 0)
                                || (keys[i].getTimestamp() > this.timestamp)
                                || (! columnMatch(i)))) {
                            getNext(i);
                        }
                    }
                }
                return insertedItem;
            } finally {
                this.lock.readLock().unlock();
            }
        }

        /*
         * @return An instance of <code>ViableRow</code>
         * @throws IOException
         */
        private ViableRow getNextViableRow() throws IOException {
            // Find the next viable row label (and timestamp).
            Text viableRow = null;
            long viableTimestamp = -1;
            for(int i = 0; i < keys.length; i++) {
                // The first key that we find that matches may have a timestamp greater
                // than the one we're looking for. We have to advance to see if there
                // is an older version present, since timestamps are sorted descending
                while (keys[i] != null &&
                        keys[i].getTimestamp() > this.timestamp &&
                        columnMatch(i) &&
                        getNext(i)) {
                    if (columnMatch(i)) {
                        break;
                    }
                }
                if((keys[i] != null)
                        // If we get here and keys[i] is not null, we already know that the
                        // column matches and the timestamp of the row is less than or equal
                        // to this.timestamp, so we do not need to test that here
                        && ((viableRow == null)
                        || (keys[i].getRow().compareTo(viableRow) < 0)
                        || ((keys[i].getRow().compareTo(viableRow) == 0)
                        && (keys[i].getTimestamp() > viableTimestamp)))) {
                    viableRow = new Text(keys[i].getRow());
                    viableTimestamp = keys[i].getTimestamp();
                }
            }
            return new ViableRow(viableRow, viableTimestamp);
        }


        // Data stucture to hold next, viable row (and timestamp).
        class ViableRow {
            private final Text row;
            private final long ts;
            ViableRow(final Text r, final long t) {
                this.row = r;
                this.ts = t;
            }

            /** @return the row */
            public Text getRow() {
                return this.row;
            }

            /** @return the timestamp */
            public long getTimestamp() {
                return this.ts;
            }
        }

        // Implementation of ChangedReadersObserver

        /** {@inheritDoc} */
        public void updateReaders() throws IOException {
            this.lock.writeLock().lock();
            try {
                // The keys are currently lined up at the next row to fetch.  Pass in
                // the current row as 'first' row and readers will be opened and cue'd
                // up so future call to next will start here.
                ViableRow viableRow = getNextViableRow();
                openReaders(viableRow.getRow());
                LOG.debug("Replaced Scanner Readers at row " + viableRow.getRow());
            } finally {
                this.lock.writeLock().unlock();
            }
        }

        /*
         * The user didn't want to start scanning at the first row. This method
         * seeks to the requested row.
         *
         * @param i Which iterator to advance
         * @param firstRow Seek to this row
         * @return True if this is the first row or if the row was not found
         */
        private boolean findFirstRow(int i, Text firstRow) throws IOException {
            ImmutableBytesWritable ibw = new ImmutableBytesWritable();
            HStoreKey firstKey =
                    (HStoreKey)this.sfsReaders[i].getClosest(new HStoreKey(firstRow), ibw);
            if (firstKey == null) {
                // Didn't find it. Close the scanner and return TRUE
                closeSubScanner(i);
                return true;
            }
            this.vals[i] = ibw.get();
            keys[i].setRow(firstKey.getRow());
            keys[i].setColumn(firstKey.getColumn());
            keys[i].setVersion(firstKey.getTimestamp());
            return columnMatch(i);
        }

        /*
         * Get the next value from the specified reader.
         *
         * Caller must be holding a read lock.
         *
         * @param i - which reader to fetch next value from
         * @return - true if there is more data available
         */
        private boolean getNext(int i) throws IOException {
            boolean result = false;
            ImmutableBytesWritable ibw = new ImmutableBytesWritable();
            while (true) {
                if (!sfsReaders[i].next(keys[i], ibw)) {
                    closeSubScanner(i);
                    break;
                }
                if (keys[i].getTimestamp() <= this.timestamp) {
                    vals[i] = ibw.get();
                    result = true;
                    break;
                }
            }
            return result;
        }

        /* Close down the indicated reader.
         * @param i Index
         */
        private void closeSubScanner(int i) {
            try {
                if(this.sfsReaders[i] != null) {
                    try {
                        this.sfsReaders[i].close();
                    } catch(IOException e) {
                        LOG.error(storeName + " closing sub-scanner", e);
                    }
                }

            } finally {
                this.sfsReaders[i] = null;
                this.keys[i] = null;
                this.vals[i] = null;
            }
        }

        /** Shut it down! */
        public void close() {
            if (this.scannerClosed) {
                return;
            }
            deleteChangedReaderObserver(this);
            try {
                for (int i = 0; i < this.sfsReaders.length; i++) {
                    if (this.sfsReaders[i] != null) {
                        try {
                            this.sfsReaders[i].close();
                        } catch (IOException e) {
                            LOG.error(storeName + " closing scanner", e);
                        }
                    }
                }
            } finally {
                this.scannerClosed = true;
            }
        }
    }

    /*
     * Go open new Reader iterators and cue them at <code>firstRow</code>.
     * Closes existing Readers if any.
     * @param firstRow
     * @throws IOException
     */
    private void openReaders(final Text firstRow) throws IOException {
        if (this.sfsReaders != null) {
            for (int i = 0; i < this.sfsReaders.length; i++) {
                // Close readers that are not exhausted, set to null
                if (this.sfsReaders[i] != null) {
                    this.sfsReaders[i].close();
                }
            }
        }
        // Open our own copies of the Readers here inside in the scanner.
        this.sfsReaders = new MapFile.Reader[getStorefiles().size()];

        // Most recent map file should be first
        int i = sfsReaders.length - 1;
        for(HStoreFile curHSF: getStorefiles().values()) {
            sfsReaders[i--] = curHSF.getReader(fs, bloomFilter);
        }

        this.keys = new HStoreKey[sfsReaders.length];
        this.vals = new byte[sfsReaders.length][];

        // Advance the readers to the first pos.
        for (i = 0; i < sfsReaders.length; i++) {
            keys[i] = new HStoreKey();
            if (firstRow != null && firstRow.getLength() != 0) {
                if (findFirstRow(i, firstRow)) {
                    continue;
                }
            }
            while (getNext(i)) {
                if (columnMatch(i)) {
                    break;
                }
            }
        }
    }

    /**
     * For a particular column i, find all the matchers defined for the column.
     * Compare the column family and column key using the matchers. The first one
     * that matches returns true. If no matchers are successful, return false.
     *
     * @param i index into the keys array
     * @return true if any of the matchers for the column match the column family
     * and the column key.
     * @throws IOException
     */
    boolean columnMatch(int i) throws IOException {
        return columnMatch(keys[i].getColumn());
    }

    /**
     * @param key The key that matched
     * @param results All the results for <code>key</code>
     * @return true if a match was found
     * @throws IOException
     */
    @Override
    public boolean next(HStoreKey key, SortedMap<Text, byte []> results)
            throws IOException {
        if (scannerClosed) {
            return false;
        }
        this.lock.readLock().lock();
        try {
            // Find the next viable row label (and timestamp).
            ViableRow viableRow = getNextViableRow();

            // Grab all the values that match this row/timestamp
            boolean insertedItem = false;
            if (viableRow.getRow() != null) {
                key.setRow(viableRow.getRow());
                key.setVersion(viableRow.getTimestamp());
                for (int i = 0; i < keys.length; i++) {
                    // Fetch the data
                    while ((keys[i] != null)
                            && (keys[i].getRow().compareTo(viableRow.getRow()) == 0)) {

                        // If we are doing a wild card match or there are multiple matchers
                        // per column, we need to scan all the older versions of this row
                        // to pick up the rest of the family members
                        if(!isWildcardScanner()
                                && !isMultipleMatchScanner()
                                && (keys[i].getTimestamp() != viableRow.getTimestamp())) {
                            break;
                        }
                        if(columnMatch(i)) {
                            // We only want the first result for any specific family member
                            if(!results.containsKey(keys[i].getColumn())) {
                                results.put(new Text(keys[i].getColumn()), vals[i]);
                                insertedItem = true;
                            }
                        }

                        if (!getNext(i)) {
                            closeSubScanner(i);
                        }
                    }
                    // Advance the current scanner beyond the chosen row, to
                    // a valid timestamp, so we're ready next time.
                    while ((keys[i] != null)
                            && ((keys[i].getRow().compareTo(viableRow.getRow()) <= 0)
                            || (keys[i].getTimestamp() > this.timestamp)
                            || (! columnMatch(i)))) {
                        getNext(i);
                    }
                }
            }
            return insertedItem;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /*
     * @return An instance of <code>ViableRow</code>
     * @throws IOException
     */
    private ViableRow getNextViableRow() throws IOException {
        // Find the next viable row label (and timestamp).
        Text viableRow = null;
        long viableTimestamp = -1;
        for(int i = 0; i < keys.length; i++) {
            // The first key that we find that matches may have a timestamp greater
            // than the one we're looking for. We have to advance to see if there
            // is an older version present, since timestamps are sorted descending
            while (keys[i] != null &&
                    keys[i].getTimestamp() > this.timestamp &&
                    columnMatch(i) &&
                    getNext(i)) {
                if (columnMatch(i)) {
                    break;
                }
            }
            if((keys[i] != null)
                    // If we get here and keys[i] is not null, we already know that the
                    // column matches and the timestamp of the row is less than or equal
                    // to this.timestamp, so we do not need to test that here
                    && ((viableRow == null)
                    || (keys[i].getRow().compareTo(viableRow) < 0)
                    || ((keys[i].getRow().compareTo(viableRow) == 0)
                    && (keys[i].getTimestamp() > viableTimestamp)))) {
                viableRow = new Text(keys[i].getRow());
                viableTimestamp = keys[i].getTimestamp();
            }
        }
        return new ViableRow(viableRow, viableTimestamp);
    }

    /** {@inheritDoc} */
    public void updateReaders() throws IOException {
        this.lock.writeLock().lock();
        try {
            // The keys are currently lined up at the next row to fetch.  Pass in
            // the current row as 'first' row and readers will be opened and cue'd
            // up so future call to next will start here.
            ViableRow viableRow = getNextViableRow();
            openReaders(viableRow.getRow());
            LOG.debug("Replaced Scanner Readers at row " + viableRow.getRow());
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /*
     * The user didn't want to start scanning at the first row. This method
     * seeks to the requested row.
     *
     * @param i Which iterator to advance
     * @param firstRow Seek to this row
     * @return True if this is the first row or if the row was not found
     */
    private boolean findFirstRow(int i, Text firstRow) throws IOException {
        ImmutableBytesWritable ibw = new ImmutableBytesWritable();
        HStoreKey firstKey =
                (HStoreKey)this.sfsReaders[i].getClosest(new HStoreKey(firstRow), ibw);
        if (firstKey == null) {
            // Didn't find it. Close the scanner and return TRUE
            closeSubScanner(i);
            return true;
        }
        this.vals[i] = ibw.get();
        keys[i].setRow(firstKey.getRow());
        keys[i].setColumn(firstKey.getColumn());
        keys[i].setVersion(firstKey.getTimestamp());
        return columnMatch(i);
    }

    /*
     * Get the next value from the specified reader.
     *
     * Caller must be holding a read lock.
     *
     * @param i - which reader to fetch next value from
     * @return - true if there is more data available
     */
    private boolean getNext(int i) throws IOException {
        boolean result = false;
        ImmutableBytesWritable ibw = new ImmutableBytesWritable();
        while (true) {
            if (!sfsReaders[i].next(keys[i], ibw)) {
                closeSubScanner(i);
                break;
            }
            if (keys[i].getTimestamp() <= this.timestamp) {
                vals[i] = ibw.get();
                result = true;
                break;
            }
        }
        return result;
    }

    /* Close down the indicated reader.
     * @param i Index
     */
    private void closeSubScanner(int i) {
        try {
            if(this.sfsReaders[i] != null) {
                try {
                    this.sfsReaders[i].close();
                } catch(IOException e) {
                    LOG.error(storeName + " closing sub-scanner", e);
                }
            }

        } finally {
            this.sfsReaders[i] = null;
            this.keys[i] = null;
            this.vals[i] = null;
        }
    }

    /** Shut it down! */
    public void close() {
        if (this.scannerClosed) {
            return;
        }
        deleteChangedReaderObserver(this);
        try {
            for (int i = 0; i < this.sfsReaders.length; i++) {
                if (this.sfsReaders[i] != null) {
                    try {
                        this.sfsReaders[i].close();
                    } catch (IOException e) {
                        LOG.error(storeName + " closing scanner", e);
                    }
                }
            }
        } finally {
            this.scannerClosed = true;
        }
    }
}
class HStoreScanner{
    /** @return true if the scanner is a wild card scanner */
    public boolean isWildcardScanner() {
        return wildcardMatch;
    }

    /** @return true if the scanner is a multiple match scanner */
    public boolean isMultipleMatchScanner() {
        return multipleMatchers;
    }

    /** {@inheritDoc} */
    public boolean next(HStoreKey key, SortedMap<Text, byte[]> results)
            throws IOException {

        // Filtered flag is set by filters.  If a cell has been 'filtered out'
        // -- i.e. it is not to be returned to the caller -- the flag is 'true'.
        boolean filtered = true;
        boolean moreToFollow = true;
        while (filtered && moreToFollow) {
            // Find the lowest-possible key.
            Text chosenRow = null;
            long chosenTimestamp = -1;
            for (int i = 0; i < this.keys.length; i++) {
                if (scanners[i] != null &&
                        (chosenRow == null ||
                                (keys[i].getRow().compareTo(chosenRow) < 0) ||
                                ((keys[i].getRow().compareTo(chosenRow) == 0) &&
                                        (keys[i].getTimestamp() > chosenTimestamp)))) {
                    chosenRow = new Text(keys[i].getRow());
                    chosenTimestamp = keys[i].getTimestamp();
                }
            }

            // Filter whole row by row key?
            filtered = dataFilter != null? dataFilter.filter(chosenRow) : false;

            // Store the key and results for each sub-scanner. Merge them as
            // appropriate.
            if (chosenTimestamp >= 0 && !filtered) {
                // Here we are setting the passed in key with current row+timestamp
                key.setRow(chosenRow);
                key.setVersion(chosenTimestamp);
                key.setColumn(HConstants.EMPTY_TEXT);
                // Keep list of deleted cell keys within this row.  We need this
                // because as we go through scanners, the delete record may be in an
                // early scanner and then the same record with a non-delete, non-null
                // value in a later. Without history of what we've seen, we'll return
                // deleted values. This List should not ever grow too large since we
                // are only keeping rows and columns that match those set on the
                // scanner and which have delete values.  If memory usage becomes a
                // problem, could redo as bloom filter.
                List<HStoreKey> deletes = new ArrayList<HStoreKey>();
                for (int i = 0; i < scanners.length && !filtered; i++) {
                    while ((scanners[i] != null
                            && !filtered
                            && moreToFollow)
                            && (keys[i].getRow().compareTo(chosenRow) == 0)) {
                        // If we are doing a wild card match or there are multiple
                        // matchers per column, we need to scan all the older versions of
                        // this row to pick up the rest of the family members
                        if (!wildcardMatch
                                && !multipleMatchers
                                && (keys[i].getTimestamp() != chosenTimestamp)) {
                            break;
                        }

                        // NOTE: We used to do results.putAll(resultSets[i]);
                        // but this had the effect of overwriting newer
                        // values with older onms. So now we only insert
                        // a result if the map does not contain the key.
                        HStoreKey hsk = new HStoreKey(key.getRow(), EMPTY_TEXT,
                                key.getTimestamp());
                        for (Map.Entry<Text, byte[]> e : resultSets[i].entrySet()) {
                            hsk.setColumn(e.getKey());
                            if (HLogEdit.isDeleted(e.getValue())) {
                                if (!deletes.contains(hsk)) {
                                    // Key changes as we cycle the for loop so add a copy to
                                    // the set of deletes.
                                    deletes.add(new HStoreKey(hsk));
                                }
                            } else if (!deletes.contains(hsk) &&
                                    !filtered &&
                                    moreToFollow &&
                                    !results.containsKey(e.getKey())) {
                                if (dataFilter != null) {
                                    // Filter whole row by column data?
                                    filtered =
                                            dataFilter.filter(chosenRow, e.getKey(), e.getValue());
                                    if (filtered) {
                                        results.clear();
                                        break;
                                    }
                                }
                                results.put(e.getKey(), e.getValue());
                            }
                        }
                        resultSets[i].clear();
                        if (!scanners[i].next(keys[i], resultSets[i])) {
                            closeScanner(i);
                        }
                    }
                }
            }

            for (int i = 0; i < scanners.length; i++) {
                // If the current scanner is non-null AND has a lower-or-equal
                // row label, then its timestamp is bad. We need to advance it.
                while ((scanners[i] != null) &&
                        (keys[i].getRow().compareTo(chosenRow) <= 0)) {
                    resultSets[i].clear();
                    if (!scanners[i].next(keys[i], resultSets[i])) {
                        closeScanner(i);
                    }
                }
            }

            moreToFollow = chosenTimestamp >= 0;

            if (dataFilter != null) {
                if (dataFilter.filterAllRemaining()) {
                    moreToFollow = false;
                }
            }

            if (results.size() <= 0 && !filtered) {
                // There were no results found for this row.  Marked it as
                // 'filtered'-out otherwise we will not move on to the next row.
                filtered = true;
            }
        }

        // If we got no results, then there is no more to follow.
        if (results == null || results.size() <= 0) {
            moreToFollow = false;
        }

        // Make sure scanners closed if no more results
        if (!moreToFollow) {
            for (int i = 0; i < scanners.length; i++) {
                if (null != scanners[i]) {
                    closeScanner(i);
                }
            }
        }

        return moreToFollow;
    }


    /** Shut down a single scanner */
    void closeScanner(int i) {
        try {
            try {
                scanners[i].close();
            } catch (IOException e) {
                LOG.warn(storeName + " failed closing scanner " + i, e);
            }
        } finally {
            scanners[i] = null;
            keys[i] = null;
            resultSets[i] = null;
        }
    }

    /** {@inheritDoc} */
    public void close() {
        for(int i = 0; i < scanners.length; i++) {
            if(scanners[i] != null) {
                closeScanner(i);
            }
        }
    }

    /** {@inheritDoc} */
    public Iterator<Entry<HStoreKey, SortedMap<Text, byte[]>>> iterator() {
        throw new UnsupportedOperationException("Unimplemented serverside. " +
                "next(HStoreKey, StortedMap(...) is more efficient");
    }
}
class HStoreSizeCalculator{
    /*
     * Data structure to hold result of a look at store file sizes.
     */
    static class HStoreSize {
        final long aggregate;
        final long largest;
        boolean splitable;

        HStoreSize(final long a, final long l, final boolean s) {
            this.aggregate = a;
            this.largest = l;
            this.splitable = s;
        }

        long getAggregate() {
            return this.aggregate;
        }

        long getLargest() {
            return this.largest;
        }

        boolean isSplitable() {
            return this.splitable;
        }

        void setSplitable(final boolean s) {
            this.splitable = s;
        }
    }

    long getAggregate() {
        return this.aggregate;
    }

    long getLargest() {
        return this.largest;
    }

    boolean isSplitable() {
        return this.splitable;
    }

    void setSplitable(final boolean s) {
        this.splitable = s;
    }

    /*
     * @param hstoreFiles
     * @return Maximum sequence number found or -1.
     * @throws IOException
     */
    private long getMaxSequenceId(final List<HStoreFile> hstoreFiles)
            throws IOException {
        long maxSeqID = -1;
        for (HStoreFile hsf : hstoreFiles) {
            long seqid = hsf.loadInfo(fs);
            if (seqid > 0) {
                if (seqid > maxSeqID) {
                    maxSeqID = seqid;
                }
            }
        }
        return maxSeqID;
    }

    long getMaxSequenceId() {
        return this.maxSeqId;
    }
}
class HStoreUtilities{
    /**
     * Called by constructor if a bloom filter is enabled for this column family.
     * If the HStore already exists, it will read in the bloom filter saved
     * previously. Otherwise, it will create a new bloom filter.
     */
    private Filter loadOrCreateBloomFilter() throws IOException {
        Path filterFile = new Path(filterDir, BLOOMFILTER_FILE_NAME);
        Filter bloomFilter = null;
        if(fs.exists(filterFile)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("loading bloom filter for " + this.storeName);
            }

            BloomFilterDescriptor.BloomFilterType type =
                    family.getBloomFilter().filterType;

            switch(type) {

                case BLOOMFILTER:
                    bloomFilter = new BloomFilter();
                    break;

                case COUNTING_BLOOMFILTER:
                    bloomFilter = new CountingBloomFilter();
                    break;

                case RETOUCHED_BLOOMFILTER:
                    bloomFilter = new RetouchedBloomFilter();
                    break;

                default:
                    throw new IllegalArgumentException("unknown bloom filter type: " +
                            type);
            }
            FSDataInputStream in = fs.open(filterFile);
            try {
                bloomFilter.readFields(in);
            } finally {
                fs.close();
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("creating bloom filter for " + this.storeName);
            }

            BloomFilterDescriptor.BloomFilterType type =
                    family.getBloomFilter().filterType;

            switch(type) {

                case BLOOMFILTER:
                    bloomFilter = new BloomFilter(family.getBloomFilter().vectorSize,
                            family.getBloomFilter().nbHash);
                    break;

                case COUNTING_BLOOMFILTER:
                    bloomFilter =
                            new CountingBloomFilter(family.getBloomFilter().vectorSize,
                                    family.getBloomFilter().nbHash);
                    break;

                case RETOUCHED_BLOOMFILTER:
                    bloomFilter =
                            new RetouchedBloomFilter(family.getBloomFilter().vectorSize,
                                    family.getBloomFilter().nbHash);
            }
        }
        return bloomFilter;
    }

    /**
     * Flushes bloom filter to disk
     *
     * @throws IOException
     */
    private void flushBloomFilter() throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("flushing bloom filter for " + this.storeName);
        }
        FSDataOutputStream out =
                fs.create(new Path(filterDir, BLOOMFILTER_FILE_NAME));
        try {
            bloomFilter.write(out);
        } finally {
            out.close();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("flushed bloom filter for " + this.storeName);
        }
    }

    private long internalGetFull(SortedMap<HStoreKey, byte []> map, HStoreKey key,
                                 Map<Text, Long> deletes, SortedMap<Text, byte []> results) {

        if (map.isEmpty() || key == null) {
            return -1L;
        }

        long rowtime = -1L;
        SortedMap<HStoreKey, byte []> tailMap = map.tailMap(key);
        for (Map.Entry<HStoreKey, byte []> es: tailMap.entrySet()) {
            HStoreKey itKey = es.getKey();
            Text itCol = itKey.getColumn();
            if (results.get(itCol) == null && key.matchesWithoutColumn(itKey)) {
                if (itKey.getTimestamp() != HConstants.LATEST_TIMESTAMP &&
                        itKey.getTimestamp() > rowtime) {
                    rowtime = itKey.getTimestamp();
                }
                byte [] val = tailMap.get(itKey);

                if (HLogEdit.isDeleted(val)) {
                    if (!deletes.containsKey(itCol)
                            || deletes.get(itCol).longValue() < itKey.getTimestamp()) {
                        deletes.put(new Text(itCol), itKey.getTimestamp());
                    }
                } else if (!(deletes.containsKey(itCol)
                        && deletes.get(itCol).longValue() >= itKey.getTimestamp())) {
                    results.put(new Text(itCol), val);
                }
            } else if (key.getRow().compareTo(itKey.getRow()) < 0) {
                break;
            }
        }
        return rowtime;
    }



    private void internalGetRowKeyAtOrBefore(SortedMap<HStoreKey, byte []> map,
                                             Text key, SortedMap<HStoreKey, Long> candidateKeys) {

        HStoreKey strippedKey = null;

        // we want the earliest possible to start searching from
        HStoreKey search_key = candidateKeys.isEmpty() ?
                new HStoreKey(key) : new HStoreKey(candidateKeys.firstKey().getRow());

        Iterator<HStoreKey> key_iterator = null;
        HStoreKey found_key = null;

        // get all the entries that come equal or after our search key
        SortedMap<HStoreKey, byte []> tailMap = map.tailMap(search_key);

        // if there are items in the tail map, there's either a direct match to
        // the search key, or a range of values between the first candidate key
        // and the ultimate search key (or the end of the cache)
        if (!tailMap.isEmpty() &&
                tailMap.firstKey().getRow().compareTo(key) <= 0) {
            key_iterator = tailMap.keySet().iterator();

            // keep looking at cells as long as they are no greater than the
            // ultimate search key and there's still records left in the map.
            do {
                found_key = key_iterator.next();
                if (found_key.getRow().compareTo(key) <= 0) {
                    strippedKey = stripTimestamp(found_key);
                    if (HLogEdit.isDeleted(tailMap.get(found_key))) {
                        if (candidateKeys.containsKey(strippedKey)) {
                            long bestCandidateTs =
                                    candidateKeys.get(strippedKey).longValue();
                            if (bestCandidateTs <= found_key.getTimestamp()) {
                                candidateKeys.remove(strippedKey);
                            }
                        }
                    } else {
                        candidateKeys.put(strippedKey,
                                new Long(found_key.getTimestamp()));
                    }
                }
            } while (found_key.getRow().compareTo(key) <= 0
                    && key_iterator.hasNext());
        } else {
            // the tail didn't contain any keys that matched our criteria, or was
            // empty. examine all the keys that preceed our splitting point.
            SortedMap<HStoreKey, byte []> headMap = map.headMap(search_key);

            // if we tried to create a headMap and got an empty map, then there are
            // no keys at or before the search key, so we're done.
            if (headMap.isEmpty()) {
                return;
            }

            // if there aren't any candidate keys at this point, we need to search
            // backwards until we find at least one candidate or run out of headMap.
            if (candidateKeys.isEmpty()) {
                HStoreKey[] cells =
                        headMap.keySet().toArray(new HStoreKey[headMap.keySet().size()]);

                Text lastRowFound = null;
                for(int i = cells.length - 1; i >= 0; i--) {
                    HStoreKey thisKey = cells[i];

                    // if the last row we found a candidate key for is different than
                    // the row of the current candidate, we can stop looking.
                    if (lastRowFound != null && !lastRowFound.equals(thisKey.getRow())) {
                        break;
                    }

                    // if this isn't a delete, record it as a candidate key. also
                    // take note of the row of this candidate so that we'll know when
                    // we cross the row boundary into the previous row.
                    if (!HLogEdit.isDeleted(headMap.get(thisKey))) {
                        lastRowFound = thisKey.getRow();
                        candidateKeys.put(stripTimestamp(thisKey),
                                new Long(thisKey.getTimestamp()));
                    }
                }
            } else {
                // if there are already some candidate keys, we only need to consider
                // the very last row's worth of keys in the headMap, because any
                // smaller acceptable candidate keys would have caused us to start
                // our search earlier in the list, and we wouldn't be searching here.
                SortedMap<HStoreKey, byte[]> thisRowTailMap =
                        headMap.tailMap(new HStoreKey(headMap.lastKey().getRow()));

                key_iterator = thisRowTailMap.keySet().iterator();

                do {
                    found_key = key_iterator.next();

                    if (HLogEdit.isDeleted(thisRowTailMap.get(found_key))) {
                        strippedKey = stripTimestamp(found_key);
                        if (candidateKeys.containsKey(strippedKey)) {
                            long bestCandidateTs =
                                    candidateKeys.get(strippedKey).longValue();
                            if (bestCandidateTs <= found_key.getTimestamp()) {
                                candidateKeys.remove(strippedKey);
                            }
                        }
                    } else {
                        candidateKeys.put(stripTimestamp(found_key),
                                found_key.getTimestamp());
                    }
                } while (key_iterator.hasNext());
            }
        }
    }

    /**
     * Examine a single map for the desired key.
     *
     * TODO - This is kinda slow.  We need a data structure that allows for
     * proximity-searches, not just precise-matches.
     *
     * @param map
     * @param key
     * @param numVersions
     * @return Ordered list of items found in passed <code>map</code>.  If no
     * matching values, returns an empty list (does not return null).
     */
    private ArrayList<byte []> internalGet(
            final SortedMap<HStoreKey, byte []> map, final HStoreKey key,
            final int numVersions) {

        ArrayList<byte []> result = new ArrayList<byte []>();
        // TODO: If get is of a particular version -- numVersions == 1 -- we
        // should be able to avoid all of the tailmap creations and iterations
        // below.
        SortedMap<HStoreKey, byte []> tailMap = map.tailMap(key);
        for (Map.Entry<HStoreKey, byte []> es: tailMap.entrySet()) {
            HStoreKey itKey = es.getKey();
            if (itKey.matchesRowCol(key)) {
                if (!HLogEdit.isDeleted(es.getValue())) {
                    result.add(tailMap.get(itKey));
                }
                if (numVersions > 0 && result.size() >= numVersions) {
                    break;
                }
            } else {
                // By L.N. HBASE-684, map is sorted, so we can't find match any more.
                break;
            }
        }
        return result;
    }

    /*
     * @param origin Where to start searching.
     * @param versions How many versions to return. Pass
     * {@link HConstants.ALL_VERSIONS} to retrieve all.
     * @return List of all keys that are of the same row and column and of
     * equal or older timestamp.  If no keys, returns an empty List. Does not
     * return null.
     */
    private List<HStoreKey> internalGetKeys(
            final SortedMap<HStoreKey, byte []> map, final HStoreKey origin,
            final int versions) {

        List<HStoreKey> result = new ArrayList<HStoreKey>();
        SortedMap<HStoreKey, byte []> tailMap = map.tailMap(origin);
        for (Map.Entry<HStoreKey, byte []> es: tailMap.entrySet()) {
            HStoreKey key = es.getKey();

            // if there's no column name, then compare rows and timestamps
            if (origin.getColumn() == null || origin.getColumn().getLength() == 0) {
                // if the current and origin row don't match, then we can jump
                // out of the loop entirely.
                if (!key.getRow().equals(origin.getRow())) {
                    break;
                }
                // if the rows match but the timestamp is newer, skip it so we can
                // get to the ones we actually want.
                if (key.getTimestamp() > origin.getTimestamp()) {
                    continue;
                }
            }
            else{ // compare rows and columns
                // if the key doesn't match the row and column, then we're done, since
                // all the cells are ordered.
                if (!key.matchesRowCol(origin)) {
                    break;
                }
            }

            if (!HLogEdit.isDeleted(es.getValue())) {
                result.add(key);
                if (versions != HConstants.ALL_VERSIONS && result.size() >= versions) {
                    // We have enough results.  Return.
                    break;
                }
            }
        }
        return result;
    }


    /**
     * @param key
     * @return True if an entry and its content is {@link HGlobals.deleteBytes}.
     * Use checking values in store. On occasion the memcache has the fact that
     * the cell has been deleted.
     */
    boolean isDeleted(final HStoreKey key) {
        return HLogEdit.isDeleted(this.mc.get(key));
    }

    static HStoreKey stripTimestamp(HStoreKey key) {
        return new HStoreKey(key.getRow(), key.getColumn());
    }

    /**
     * Test that the <i>target</i> matches the <i>origin</i>. If the
     * <i>origin</i> has an empty column, then it's assumed to mean any column
     * matches and only match on row and timestamp. Otherwise, it compares the
     * keys with HStoreKey.matchesRowCol().
     * @param origin The key we're testing against
     * @param target The key we're testing
     */
    private boolean cellMatches(HStoreKey origin, HStoreKey target){
        // if the origin's column is empty, then we're matching any column
        if (origin.getColumn().equals(new Text())){
            // if the row matches, then...
            if (target.getRow().equals(origin.getRow())) {
                // check the timestamp
                return target.getTimestamp() <= origin.getTimestamp();
            }
            return false;
        }
        // otherwise, we want to match on row and column
        return target.matchesRowCol(origin);
    }

    /**
     * Test that the <i>target</i> matches the <i>origin</i>. If the <i>origin</i>
     * has an empty column, then it just tests row equivalence. Otherwise, it uses
     * HStoreKey.matchesRowCol().
     * @param origin Key we're testing against
     * @param target Key we're testing
     */
    private boolean rowMatches(HStoreKey origin, HStoreKey target){
        // if the origin's column is empty, then we're matching any column
        if (origin.getColumn().equals(new Text())){
            // if the row matches, then...
            return target.getRow().equals(origin.getRow());
        }
        // otherwise, we want to match on row and column
        return target.matchesRowCol(origin);
    }
}