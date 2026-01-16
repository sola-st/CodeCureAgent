class RegionAssignmentHandler {
    public void assignRegion(HRegionInfo region, boolean setOfflineInZK) {
        assign(region, setOfflineInZK, false);
    }

    public void unassignRegion(HRegionInfo region, boolean force) {
        unassign(region, force);
    }

    /**
     * Handle failover.  Restore state from META and ZK.  Handle any regions in
     * transition.  Presumes <code>.META.</code> and <code>-ROOT-</code> deployed.
     * @throws KeeperException
     * @throws IOException
     * @throws InterruptedException
     */
    void processFailover() throws KeeperException, IOException, InterruptedException {
        // Concurrency note: In the below the accesses on regionsInTransition are
        // outside of a synchronization block where usually all accesses to RIT are
        // synchronized.  The presumption is that in this case it is safe since this
        // method is being played by a single thread on startup.

        // TODO: Check list of user regions and their assignments against regionservers.
        // TODO: Regions that have a null location and are not in regionsInTransitions
        // need to be handled.

        // Add -ROOT- and .META. on regions map.  They must be deployed if we got
        // this far.  Caller takes care of it.
        HServerInfo hsi =
                this.serverManager.getHServerInfo(this.catalogTracker.getMetaLocation());
        if (hsi != null) {
            regionOnline(HRegionInfo.FIRST_META_REGIONINFO, hsi);
        }
        hsi = this.serverManager.getHServerInfo(this.catalogTracker.getRootLocation());
        if (hsi != null) {
            regionOnline(HRegionInfo.ROOT_REGIONINFO, hsi);
        }

    /**
     * Marks the region as online.  Removes it from regions in transition and
     * updates the in-memory assignment information.
     * <p>
     * Used when a region has been successfully opened on a region server.
     * @param regionInfo
     * @param serverInfo
     */
    public void regionOnline(HRegionInfo regionInfo, HServerInfo serverInfo) {
        synchronized (this.regionsInTransition) {
            RegionState rs =
                    this.regionsInTransition.remove(regionInfo.getEncodedName());
            if (rs != null) {
                this.regionsInTransition.notifyAll();
            }
        }
        synchronized (this.regions) {
            // Add check
            HServerInfo hsi = this.regions.get(regionInfo);
            if (hsi != null) LOG.warn("Overwriting " + regionInfo.getEncodedName() +
                    " on " + hsi);

            HServerInfo hsiWithoutLoad = new HServerInfo(
                    serverInfo.getServerAddress(), serverInfo.getStartCode(),
                    serverInfo.getInfoPort(), serverInfo.getHostname());

            if (isServerOnline(hsiWithoutLoad.getServerName())) {
                this.regions.put(regionInfo, hsiWithoutLoad);
                addToServers(hsiWithoutLoad, regionInfo);
                this.regions.notifyAll();
            } else {
                LOG.info("The server is not in online servers, ServerName=" +
                        hsiWithoutLoad.getServerName() + ", region=" +
                        regionInfo.getEncodedName());
            }
        }
        // Remove plan if one.
        clearRegionPlan(regionInfo);
        // Update timers for all regions in transition going against this server.
        updateTimers(serverInfo);
    }

    /**
     * Marks the region as offline.  Removes it from regions in transition and
     * removes in-memory assignment information.
     * <p>
     * Used when a region has been closed and should remain closed.
     * @param regionInfo
     */
    public void regionOffline(final HRegionInfo regionInfo) {
        synchronized(this.regionsInTransition) {
            if (this.regionsInTransition.remove(regionInfo.getEncodedName()) != null) {
                this.regionsInTransition.notifyAll();
            }
        }
        // remove the region plan as well just in case.
        clearRegionPlan(regionInfo);
        setOffline(regionInfo);
    }


   /**
     * Sets the region as offline by removing in-memory assignment information but
     * retaining transition information.
     * <p>
     * Used when a region has been closed but should be reassigned.
     * @param regionInfo
     */
    public void setOffline(HRegionInfo regionInfo) {
        synchronized (this.regions) {
            HServerInfo serverInfo = this.regions.remove(regionInfo);
            if (serverInfo == null) return;
            Set<HRegionInfo> serverRegions = this.servers.get(serverInfo);
            if (!serverRegions.remove(regionInfo)) {
                LOG.warn("No " + regionInfo + " on " + serverInfo);
            }
        }
    }

   public void offlineDisabledRegion(HRegionInfo regionInfo) {
        // Disabling so should not be reassigned, just delete the CLOSED node
        LOG.debug("Table being disabled so deleting ZK node and removing from " +
                "regions in transition, skipping assignment of region " +
                regionInfo.getRegionNameAsString());
        try {
            if (!ZKAssign.deleteClosedNode(watcher, regionInfo.getEncodedName())) {
                // Could also be in OFFLINE mode
                ZKAssign.deleteOfflineNode(watcher, regionInfo.getEncodedName());
            }
        } catch (KeeperException.NoNodeException nne) {
            LOG.debug("Tried to delete closed node for " + regionInfo + " but it " +
                    "does not exist so just offlining");
        } catch (KeeperException e) {
            this.master.abort("Error deleting CLOSED node in ZK", e);
        }
        regionOffline(regionInfo);
    }


   /**
     * @param region
     * @return
     */
    private RegionState addToRegionsInTransition(final HRegionInfo region) {
        synchronized (regionsInTransition) {
            return forceRegionStateToOffline(region);
        }
    }

   /**
     * Sets regions {@link RegionState} to {@link RegionState.State#OFFLINE}.
     * Caller must hold lock on this.regionsInTransition.
     * @param region
     * @return Amended RegionState.
     */
    private RegionState forceRegionStateToOffline(final HRegionInfo region) {
        String encodedName = region.getEncodedName();
        RegionState state = this.regionsInTransition.get(encodedName);
        if (state == null) {
            state = new RegionState(region, RegionState.State.OFFLINE);
            this.regionsInTransition.put(encodedName, state);
        } else {
            LOG.debug("Forcing OFFLINE; was=" + state);
            state.update(RegionState.State.OFFLINE);
        }
        return state;
    }

    public boolean isRegionOnline(HRegionInfo hri) {
        HServerInfo hsi = null;
        synchronized (this.regions) {
            hsi = this.regions.get(hri);
            if (hsi == null) {
                return false;
            }
            if (this.isServerOnline(hsi.getServerName())) {
                return true;
            } else {
                // Remove the assignment mapping for hsi.
                Set<HRegionInfo> hriSet = this.servers.get(hsi);
                if (hriSet != null) {
                    hriSet.remove(hri);
                }
                this.regions.remove(hri);
                return false;
            }
        }
    }

    private void debugLog(HRegionInfo region, String string) {
        if (region.isMetaTable() || region.isRootRegion()) {
            LOG.info(string);
        } else {
            LOG.debug(string);
        }
    }

    /**
     * Assigns the ROOT region.
     * <p>
     * Assumes that ROOT is currently closed and is not being actively served by
     * any RegionServer.
     * <p>
     * Forcibly unsets the current root region location in ZooKeeper and assigns
     * ROOT to a random RegionServer.
     * @throws KeeperException
     */
    public void assignRoot() throws KeeperException {
        RootLocationEditor.deleteRootLocation(this.master.getZooKeeper());
        assign(HRegionInfo.ROOT_REGIONINFO, true);
    }


    /**
     * Assigns the META region.
     * <p>
     * Assumes that META is currently closed and is not being actively served by
     * any RegionServer.
     * <p>
     * Forcibly assigns META to a random RegionServer.
     */
    public void assignMeta() {
        // Force assignment to a random server
        assign(HRegionInfo.FIRST_META_REGIONINFO, true);
    }

     /**
     * Assigns all user regions, if any.  Used during cluster startup.
     * <p>
     * This is a synchronous call and will return once every region has been
     * assigned.  If anything fails, an exception is thrown and the cluster
     * should be shutdown.
     * @throws InterruptedException
     * @throws IOException
     */
    public void assignAllUserRegions() throws IOException, InterruptedException {
        // Get all available servers
        List<HServerInfo> servers = serverManager.getOnlineServersList();

        // Scan META for all user regions, skipping any disabled tables
        Map<HRegionInfo,HServerAddress> allRegions =
                MetaReader.fullScan(catalogTracker, this.zkTable.getDisabledTables(), true);
        if (allRegions == null || allRegions.isEmpty()) return;

        // Determine what type of assignment to do on startup
        boolean retainAssignment = master.getConfiguration().
                getBoolean("hbase.master.startup.retainassign", true);

        Map<HServerInfo, List<HRegionInfo>> bulkPlan = null;
        if (retainAssignment) {
            // Reuse existing assignment info
            bulkPlan = LoadBalancer.retainAssignment(allRegions, servers);
        } else {
            // assign regions in round-robin fashion
            bulkPlan = LoadBalancer.roundRobinAssignment(new ArrayList<HRegionInfo>(allRegions.keySet()), servers);
        }
        LOG.info("Bulk assigning " + allRegions.size() + " region(s) across " +
                servers.size() + " server(s), retainAssignment=" + retainAssignment);

        // Use fixed count thread pool assigning.
        BulkAssigner ba = new StartupBulkAssigner(this.master, bulkPlan, this);
        ba.bulkAssign();
        LOG.info("Bulk assigning done");
    }

     /**
     * Wait until no regions in transition.
     * @param timeout How long to wait.
     * @return True if nothing in regions in transition.
     * @throws InterruptedException
     */
    boolean waitUntilNoRegionsInTransition(final long timeout)
            throws InterruptedException {
        // Blocks until there are no regions in transition. It is possible that
        // there
        // are regions in transition immediately after this returns but guarantees
        // that if it returns without an exception that there was a period of time
        // with no regions in transition from the point-of-view of the in-memory
        // state of the Master.
        long startTime = System.currentTimeMillis();
        long remaining = timeout;
        synchronized (regionsInTransition) {
            while (regionsInTransition.size() > 0 && !this.master.isStopped()
                    && remaining > 0) {
                regionsInTransition.wait(remaining);
                remaining = timeout - (System.currentTimeMillis() - startTime);
            }
        }
        return regionsInTransition.isEmpty();
    }

    /**
     * Wait until no regions in transition.
     * @param timeout How long to wait.
     * @return True if nothing in regions in transition.
     * @throws InterruptedException
     */
    boolean waitUntilNoRegionsInTransition(final long timeout)
            throws InterruptedException {
        // Blocks until there are no regions in transition. It is possible that
        // there
        // are regions in transition immediately after this returns but guarantees
        // that if it returns without an exception that there was a period of time
        // with no regions in transition from the point-of-view of the in-memory
        // state of the Master.
        long startTime = System.currentTimeMillis();
        long remaining = timeout;
        synchronized (regionsInTransition) {
            while (regionsInTransition.size() > 0 && !this.master.isStopped()
                    && remaining > 0) {
                regionsInTransition.wait(remaining);
                remaining = timeout - (System.currentTimeMillis() - startTime);
            }
        }
        return regionsInTransition.isEmpty();
    }

    /**
     * Assigns list of user regions in round-robin fashion, if any.
     * @param sync True if we are to wait on all assigns.
     * @param startup True if this is server startup time.
     * @throws InterruptedException
     * @throws IOException
     */
    void bulkAssignUserRegions(final HRegionInfo [] regions,
                               final List<HServerInfo> servers, final boolean sync)
            throws IOException {
        Map<HServerInfo, List<HRegionInfo>> bulkPlan =
                LoadBalancer.roundRobinAssignment(java.util.Arrays.asList(regions), servers);
        LOG.info("Bulk assigning " + regions.length + " region(s) " +
                "round-robin across " + servers.size() + " server(s)");
        // Use fixed count thread pool assigning.
        BulkAssigner ba = new GeneralBulkAssigner(this.master, bulkPlan, this);
        try {
            ba.bulkAssign(sync);
        } catch (InterruptedException e) {
            throw new IOException("InterruptedException bulk assigning", e);
        }
        LOG.info("Bulk assigning done");
    }
}
class RegionTransitionManager {
    private final ConcurrentSkipListMap<String, RegionState> regionsInTransition = new ConcurrentSkipListMap<>();

    public void addRegionInTransition(String encodedRegionName, RegionState regionState) {
        regionsInTransition.put(encodedRegionName, regionState);
    }

    public RegionState getRegionState(String encodedRegionName) {
        return regionsInTransition.get(encodedRegionName);
    }

    public void removeRegionInTransition(String encodedRegionName) {
        regionsInTransition.remove(encodedRegionName);
    }

    public boolean isRegionInTransition(String encodedRegionName) {
        return regionsInTransition.containsKey(encodedRegionName);
    }

   /**
     * Put the region <code>hri</code> into an offline state up in zk.
     * @param hri
     * @param oldData
     * @throws KeeperException
     */
    private void forceOffline(final HRegionInfo hri,
                              final RegionTransitionData oldData)
            throws KeeperException {
        // If was on dead server, its closed now.  Force to OFFLINE and then
        // handle it like a close; this will get it reassigned if appropriate
        debugLog(hri, "RIT " + hri.getEncodedName() + " in state=" +
                oldData.getEventType() + " was on deadserver; forcing offline");
        ZKAssign.createOrForceNodeOffline(this.watcher, hri,
                this.master.getServerName());
        addToRITandCallClose(hri, RegionState.State.OFFLINE, oldData);
    }

     /**
     * Add to the in-memory copy of regions in transition and then call close
     * handler on passed region <code>hri</code>
     * @param hri
     * @param state
     * @param oldData
     */
    private void addToRITandCallClose(final HRegionInfo hri,
                                      final RegionState.State state, final RegionTransitionData oldData) {
        this.regionsInTransition.put(hri.getEncodedName(),
                new RegionState(hri, state, oldData.getStamp()));
        new ClosedRegionHandler(this.master, this, hri).process();
    }

     /**
     * Process all regions that are in transition up in zookeeper.  Used by
     * master joining an already running cluster.
     * @throws KeeperException
     * @throws IOException
     * @throws InterruptedException
     */
    void processRegionsInTransition()
            throws KeeperException, IOException, InterruptedException {
        // Pass null to signify no dead servers in this context.
        processRegionsInTransition(null);
    }


    /**
     * Process all regions that are in transition up in zookeeper.  Used by
     * master joining an already running cluster.
     * @throws KeeperException
     * @throws IOException
     * @throws InterruptedException
     */
    void processRegionsInTransition()
            throws KeeperException, IOException, InterruptedException {
        // Pass null to signify no dead servers in this context.
        processRegionsInTransition(null);
    }


    /**
     * If region is up in zk in transition, then do fixup and block and wait until
     * the region is assigned and out of transition.  Used on startup for
     * catalog regions.
     * @param hri Region to look for.
     * @return True if we processed a region in transition else false if region
     * was not up in zk in transition.
     * @throws InterruptedException
     * @throws KeeperException
     * @throws IOException
     */
    boolean processRegionInTransitionAndBlockUntilAssigned(final HRegionInfo hri)
            throws InterruptedException, KeeperException, IOException {
        boolean intransistion =
                processRegionInTransition(hri.getEncodedName(), hri, null);
        if (!intransistion) return intransistion;
        debugLog(hri, "Waiting on " + HRegionInfo.prettyPrint(hri.getEncodedName()));
        synchronized(this.regionsInTransition) {
            while (!this.master.isStopped() &&
                    this.regionsInTransition.containsKey(hri.getEncodedName())) {
                this.regionsInTransition.wait();
            }
        }
        return intransistion;
    }

     /**
     * If region is up in zk in transition, then do fixup and block and wait until
     * the region is assigned and out of transition.  Used on startup for
     * catalog regions.
     * @param hri Region to look for.
     * @return True if we processed a region in transition else false if region
     * was not up in zk in transition.
     * @throws InterruptedException
     * @throws KeeperException
     * @throws IOException
     */
    boolean processRegionInTransitionAndBlockUntilAssigned(final HRegionInfo hri)
            throws InterruptedException, KeeperException, IOException {
        boolean intransistion =
                processRegionInTransition(hri.getEncodedName(), hri, null);
        if (!intransistion) return intransistion;
        debugLog(hri, "Waiting on " + HRegionInfo.prettyPrint(hri.getEncodedName()));
        synchronized(this.regionsInTransition) {
            while (!this.master.isStopped() &&
                    this.regionsInTransition.containsKey(hri.getEncodedName())) {
                this.regionsInTransition.wait();
            }
        }
        return intransistion;
    }


    void processRegionsInTransition(final RegionTransitionData data,
                                    final HRegionInfo regionInfo,
                                    final Map<String, List<Pair<HRegionInfo, Result>>> deadServers,
                                    int expectedVersion)
            throws KeeperException {
        String encodedRegionName = regionInfo.getEncodedName();
        LOG.info("Processing region " + regionInfo.getRegionNameAsString() +
                " in state " + data.getEventType());
        synchronized (regionsInTransition) {
            switch (data.getEventType()) {
                case RS_ZK_REGION_CLOSING:
                    //If zk node of the region was updated by a live server,
                    //we should skip this region and just add it into RIT.
                    if (isOnDeadServer(regionInfo, deadServers) &&
                            (null == data.getServerName() ||
                                    !serverManager.isServerOnline(data.getServerName()))){
                        // If was on dead server, its closed now.  Force to OFFLINE and this
                        // will get it reassigned if appropriate
                        forceOffline(regionInfo, data);
                    } else {
                        // Just insert region into RIT.
                        // If this never updates the timeout will trigger new assignment
                        regionsInTransition.put(encodedRegionName, new RegionState(
                                regionInfo, RegionState.State.CLOSING, data.getStamp()));
                    }
                    break;

                case RS_ZK_REGION_CLOSED:
                case RS_ZK_REGION_FAILED_OPEN:
                    // Region is closed, insert into RIT and handle it
                    addToRITandCallClose(regionInfo, RegionState.State.CLOSED, data);
                    break;

                case M_ZK_REGION_OFFLINE:
                    // Region is offline, insert into RIT and handle it like a closed
                    addToRITandCallClose(regionInfo, RegionState.State.OFFLINE, data);
                    break;

                case RS_ZK_REGION_OPENING:
                    // TODO: Could check if it was on deadServers.  If it was, then we could
                    // do what happens in TimeoutMonitor when it sees this condition.

                    // Just insert region into RIT
                    // If this never updates the timeout will trigger new assignment
                    regionsInTransition.put(encodedRegionName, new RegionState(
                            regionInfo, RegionState.State.OPENING, data.getStamp()));
                    break;

                case RS_ZK_REGION_OPENED:
                    // Region is opened, insert into RIT and handle it
                    regionsInTransition.put(encodedRegionName, new RegionState(
                            regionInfo, RegionState.State.OPEN, data.getStamp()));
                    String sn = data.getServerName();
                    // hsi could be null if this server is no longer online.  If
                    // that the case, just let this RIT timeout; it'll be assigned
                    // to new server then.
                    if (sn == null) {
                        LOG.warn("Region in transition " + regionInfo.getEncodedName() +
                                " references a server no longer up " + data.getServerName() +
                                "; letting RIT timeout so will be assigned elsewhere");
                        break;
                    }
                    if ((!serverManager.isServerOnline(sn) || (null == data.getServerName()))
                            && (isOnDeadServer(regionInfo, deadServers)
                            || regionInfo.isMetaRegion() || regionInfo.isRootRegion())) {
                        // If was on a dead server, then its not open any more; needs handling.
                        forceOffline(regionInfo, data);
                    } else {
                        HServerInfo hsi = this.serverManager.getServerInfo(sn);
                        if (hsi == null) {
                            LOG.info("Failed to find " + sn +
                                    " in list of online servers; skipping registration of open of " +
                                    regionInfo.getRegionNameAsString());
                        } else {
                            new OpenedRegionHandler(master, this, regionInfo, hsi,
                                    expectedVersion).process();
                        }
                    }
                    break;
            }
        }
    }

   /**
     * Clears the specified region from being in transition.
     * <p>
     * @param hri Region to remove.
     * @deprecated This is a dupe of {@link #regionOffline(HRegionInfo)}.
     *   Please use that method instead.
     */
    public void clearRegionFromTransition(HRegionInfo hri) {
        synchronized (this.regionsInTransition) {
            this.regionsInTransition.remove(hri.getEncodedName());
        }
        synchronized (this.regions) {
            this.regions.remove(hri);
            for (Set<HRegionInfo> regions : this.servers.values()) {
                regions.remove(hri);
            }
        }
        clearRegionPlan(hri);
    }


     /**
     * @param region Region whose plan we are to clear.
     */
    void clearRegionPlan(final HRegionInfo region) {
        synchronized (this.regionPlans) {
            this.regionPlans.remove(region.getEncodedName());
        }
    }

     /**
     * @return A copy of the Map of regions currently in transition.
     */
    public NavigableMap<String, RegionState> getRegionsInTransition() {
        synchronized (this.regionsInTransition) {
            return new TreeMap<String, RegionState>(this.regionsInTransition);
        }
    }

    /**
     * @return True if regions in transition.
     */
    public boolean isRegionsInTransition() {
        synchronized (this.regionsInTransition) {
            return !this.regionsInTransition.isEmpty();
        }
    }

    /**
     * @param hri Region to check.
     * @return Returns null if passed region is not in transition else the current
     * RegionState
     */
    public RegionState isRegionInTransition(final HRegionInfo hri) {
        synchronized (this.regionsInTransition) {
            return this.regionsInTransition.get(hri.getEncodedName());
        }
    }

    /**
     * Wait on region to clear regions-in-transition.
     * @param hri Region to wait on.
     * @throws IOException
     */
    public void waitOnRegionToClearRegionsInTransition(final HRegionInfo hri)
            throws IOException {
        if (isRegionInTransition(hri) == null) return;
        RegionState rs = null;
        // There is already a timeout monitor on regions in transition so I
        // should not have to have one here too?
        while(!this.master.isStopped() && (rs = isRegionInTransition(hri)) != null) {
            Threads.sleep(1000);
            LOG.info("Waiting on " + rs + " to clear regions-in-transition");
        }
        if (this.master.isStopped()) {
            LOG.info("Giving up wait on regions in " +
                    "transition because stoppable.isStopped is set");
        }
    }
}
class ZooKeeperEventHandler {
    private ZooKeeperWatcher watcher;

    public ZooKeeperEventHandler(ZooKeeperWatcher watcher) {
        this.watcher = watcher;
    }



    public void handleNodeDataChanged(String path) {
        // Implementation for handleNodeDataChanged
        // Add the content of the original nodeDataChanged method here
    }

    public void handleNodeChildrenChanged(String path) {
        // Implementation for handleNodeChildrenChanged
        // Add the content of the original nodeChildrenChanged method here
    }

    /**
     * New unassigned node has been created.
     *
     * <p>This happens when an RS begins the OPENING or CLOSING of a region by
     * creating an unassigned node.
     *
     * <p>When this happens we must:
     * <ol>
     *   <li>Watch the node for further events</li>
     *   <li>Read and handle the state in the node</li>
     * </ol>
     */
    @Override
    public void nodeCreated(String path) {
        if(path.startsWith(watcher.assignmentZNode)) {
            synchronized(regionsInTransition) {
                try {
                    Stat stat = new Stat();
                    RegionTransitionData data = ZKAssign.getDataAndWatch(watcher, path,
                            stat);
                    if(data == null) {
                        return;
                    }
                    handleRegion(data, stat.getVersion());
                } catch (KeeperException e) {
                    master.abort("Unexpected ZK exception reading unassigned node data", e);
                }
            }
        }
    }

     /**
     * Existing unassigned node has had data changed.
     *
     * <p>This happens when an RS transitions from OFFLINE to OPENING, or between
     * OPENING/OPENED and CLOSING/CLOSED.
     *
     * <p>When this happens we must:
     * <ol>
     *   <li>Watch the node for further events</li>
     *   <li>Read and handle the state in the node</li>
     * </ol>
     */
    @Override
    public void nodeDataChanged(String path) {
        if(path.startsWith(watcher.assignmentZNode)) {
            synchronized(regionsInTransition) {
                try {
                    Stat stat = new Stat();
                    RegionTransitionData data = ZKAssign.getDataAndWatch(watcher, path,
                            stat);
                    if(data == null) {
                        return;
                    }
                    handleRegion(data, stat.getVersion());
                } catch (KeeperException e) {
                    master.abort("Unexpected ZK exception reading unassigned node data", e);
                }
            }
        }
    }

    /**
     * New unassigned node has been created.
     *
     * <p>This happens when an RS begins the OPENING or CLOSING of a region by
     * creating an unassigned node.
     *
     * <p>When this happens we must:
     * <ol>
     *   <li>Watch the node for further children changed events</li>
     *   <li>Watch all new children for changed events</li>
     *   <li>Read all children and handle them</li>
     * </ol>
     */
    @Override
    public void nodeChildrenChanged(String path) {
        if(path.equals(watcher.assignmentZNode)) {
            synchronized(regionsInTransition) {
                try {
                    List<NodeAndData> newNodes = ZKUtil.watchAndGetNewChildren(watcher,
                            watcher.assignmentZNode);
                    for(NodeAndData newNode : newNodes) {
                        LOG.debug("Handling new unassigned node: " + newNode);
                        handleRegion(RegionTransitionData.fromBytes(newNode.getData()),
                                newNode.getVersion());
                    }
                } catch(KeeperException e) {
                    master.abort("Unexpected ZK exception reading unassigned children", e);
                }
            }
        }
    }
}

public class AssignmentManager extends ZooKeeperListener {
    private Server master;
    private ServerManager serverManager;
    private CatalogTracker catalogTracker;
    private TimeoutMonitor timeoutMonitor;
    private final ZKTable zkTable;
    private final int maximumAssignmentAttempts;

    private RegionAssignmentHandler regionAssignmentHandler = new RegionAssignmentHandler();
    private RegionTransitionManager regionTransitionManager = new RegionTransitionManager();
    private ZooKeeperEventHandler zooKeeperEventHandler;

    public AssignmentManager(Server master, ServerManager serverManager, CatalogTracker catalogTracker, final ExecutorService service) throws KeeperException {
        super(master.getZooKeeper());
        this.master = master;
        this.serverManager = serverManager;
        this.catalogTracker = catalogTracker;
        this.timeoutMonitor = new TimeoutMonitor(master.getConfiguration().getInt("hbase.master.assignment.timeoutmonitor.period", 10000), master, master.getConfiguration().getInt("hbase.master.assignment.timeoutmonitor.timeout", 1800000), serverManager);
        this.zkTable = new ZKTable(this.master.getZooKeeper());
        this.maximumAssignmentAttempts = this.master.getConfiguration().getInt("hbase.assignment.maximum.attempts", 10);
        this.zooKeeperEventHandler = new ZooKeeperEventHandler(this.master.getZooKeeper());
    }

    void startTimeOutMonitor() {
        Threads.setDaemonThreadRunning(timeoutMonitor,
                master.getServerName() + ".timeoutMonitor");
    }


    /**
     * @return Instance of ZKTable.
     */
    public ZKTable getZKTable() {
        // These are 'expensive' to make involving trip to zk ensemble so allow
        // sharing.
        return this.zkTable;
    }


     /**
     * Reset all unassigned znodes.  Called on startup of master.
     * Call {@link #assignAllUserRegions()} after root and meta have been assigned.
     * @throws IOException
     * @throws KeeperException
     */
    void cleanoutUnassigned() throws IOException, KeeperException {
        // Cleanup any existing ZK nodes and start watching
        ZKAssign.deleteAllNodes(watcher);
        ZKUtil.listChildrenAndWatchForNewChildren(this.watcher,
                this.watcher.assignmentZNode);
    }

   /**
     * Waits until the specified region has completed assignment.
     * <p>
     * If the region is already assigned, returns immediately.  Otherwise, method
     * blocks until the region is assigned.
     * @param regionInfo region to wait on assignment for
     * @throws InterruptedException
     */
    public void waitForAssignment(HRegionInfo regionInfo)
            throws InterruptedException {
        synchronized(regions) {
            while(!regions.containsKey(regionInfo)) {
                regions.wait();
            }
        }
    }

     public void stop() {
        this.timeoutMonitor.interrupt();
    }

    /**
     * Check whether the RegionServer is online.
     */
    public boolean isServerOnline(String serverName) {
        return this.serverManager.isServerOnline(serverName);
    }

    /**
     * Process shutdown server removing any assignments.
     *
     * @param hsi Server that went down.
     * @return list of regions in transition and region plans on this server
     */
    public RegionsOnDeadServer processServerShutdown(final HServerInfo hsi) {
        RegionsOnDeadServer regionsOnDeadServer = new RegionsOnDeadServer();
        Set<HRegionInfo> regionsFromRegionPlansForServer = new HashSet<HRegionInfo>();
        // Clean out any existing assignment plans for this server
        synchronized (this.regionPlans) {
            for (Iterator <Map.Entry<String, RegionPlan>> i =
                 this.regionPlans.entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, RegionPlan> e = i.next();
                HServerInfo otherHsi = e.getValue().getDestination();
                // The HSI will be null if the region is planned for a random assign.
                if (otherHsi != null && otherHsi.equals(hsi)) {
                    // Store the related regions in regionPlans.
                    regionsFromRegionPlansForServer.add(e.getValue().getRegionInfo());
                    // Use iterator's remove else we'll get CME
                    i.remove();
                }
            }
        }

    /**
     * Update inmemory structures.
     * @param hsi Server that reported the split
     * @param parent Parent region that was split
     * @param a Daughter region A
     * @param b Daughter region B
     */
    public void handleSplitReport(final HServerInfo hsi, final HRegionInfo parent,
                                  final HRegionInfo a, final HRegionInfo b) {
        regionOffline(parent);
        // Remove any CLOSING node, if exists, due to race between master & rs
        // for close & split.  Not putting into regionOffline method because it is
        // called from various locations.
        try {
            RegionTransitionData node = ZKAssign.getDataNoWatch(this.watcher,
                    parent.getEncodedName(), null);
            if (node != null) {
                if (node.getEventType().equals(EventType.RS_ZK_REGION_CLOSING)) {
                    ZKAssign.deleteClosingNode(this.watcher, parent);
                } else {
                    LOG.warn("Split report has RIT node (shouldnt have one): " +
                            parent + " node: " + node);
                }
            }
        } catch (KeeperException e) {
            LOG.warn("Exception while validating RIT during split report", e);
        }
        synchronized (this.regions) {
            //one daughter is already online, do nothing
            HServerInfo hsia = this.regions.get(a);
            if (hsia != null){
                LOG.warn("Trying to process the split of " +a.getEncodedName()+ ", " +
                        "but it was already done and one daughter is on region server " + hsia);
                return;
            }
        }

        regionOnline(a, hsi);
        regionOnline(b, hsi);

        // There's a possibility that the region was splitting while a user asked
        // the master to disable, we need to make sure we close those regions in
        // that case. This is not racing with the region server itself since RS
        // report is done after the split transaction completed.
        if (this.zkTable.isDisablingOrDisabledTable(
                parent.getTableDesc().getNameAsString())) {
            unassign(a);
            unassign(b);
        }
    }


    /**
     * @return A clone of current assignments. Note, this is assignments only.
     * If a new server has come in and it has no regions, it will not be included
     * in the returned Map.
     */
    Map<HServerInfo, List<HRegionInfo>> getAssignments() {
        // This is an EXPENSIVE clone.  Cloning though is the safest thing to do.
        // Can't let out original since it can change and at least the loadbalancer
        // wants to iterate this exported list.  We need to synchronize on regions
        // since all access to this.servers is under a lock on this.regions.
        Map<HServerInfo, List<HRegionInfo>> result = null;
        synchronized (this.regions) {
            result = new HashMap<HServerInfo, List<HRegionInfo>>(this.servers.size());
            for (Map.Entry<HServerInfo, Set<HRegionInfo>> e: this.servers.entrySet()) {
                List<HRegionInfo> shallowCopy = new ArrayList<HRegionInfo>(e.getValue());
                HServerInfo clone = new HServerInfo(e.getKey());
                // Set into server load the number of regions this server is carrying
                // The load balancer calculation needs it at least and its handy.
                clone.getLoad().setNumberOfRegions(e.getValue().size());
                result.put(clone, shallowCopy);
            }
        }
        return result;
    }

     /**
     * @return A clone of current assignments. Note, this is assignments only.
     * If a new server has come in and it has no regions, it will not be included
     * in the returned Map.
     */
    Map<HServerInfo, List<HRegionInfo>> getAssignments() {
        // This is an EXPENSIVE clone.  Cloning though is the safest thing to do.
        // Can't let out original since it can change and at least the loadbalancer
        // wants to iterate this exported list.  We need to synchronize on regions
        // since all access to this.servers is under a lock on this.regions.
        Map<HServerInfo, List<HRegionInfo>> result = null;
        synchronized (this.regions) {
            result = new HashMap<HServerInfo, List<HRegionInfo>>(this.servers.size());
            for (Map.Entry<HServerInfo, Set<HRegionInfo>> e: this.servers.entrySet()) {
                List<HRegionInfo> shallowCopy = new ArrayList<HRegionInfo>(e.getValue());
                HServerInfo clone = new HServerInfo(e.getKey());
                // Set into server load the number of regions this server is carrying
                // The load balancer calculation needs it at least and its handy.
                clone.getLoad().setNumberOfRegions(e.getValue().size());
                result.put(clone, shallowCopy);
            }
        }
        return result;
    }

      /**
     * @param plan Plan to execute.
     */
    void balance(final RegionPlan plan) {
        synchronized (this.regionPlans) {
            this.regionPlans.put(plan.getRegionName(), plan);
        }
        unassign(plan.getRegionInfo());
    }
}

public class AssignmentManager extends ZooKeeperListener {
    private Server master;
    private ServerManager serverManager;
    private CatalogTracker catalogTracker;
    private TimeoutMonitor timeoutMonitor;
    private final ZKTable zkTable;
    private final int maximumAssignmentAttempts;

    private RegionAssignmentHandler regionAssignmentHandler = new RegionAssignmentHandler();
    private RegionTransitionManager regionTransitionManager = new RegionTransitionManager();
    private ZooKeeperEventHandler zooKeeperEventHandler;

    public AssignmentManager(Server master, ServerManager serverManager, CatalogTracker catalogTracker, final ExecutorService service) throws KeeperException {
        super(master.getZooKeeper());
        this.master = master;
        this.serverManager = serverManager;
        this.catalogTracker = catalogTracker;
        this.timeoutMonitor = new TimeoutMonitor(master.getConfiguration().getInt("hbase.master.assignment.timeoutmonitor.period", 10000), master, master.getConfiguration().getInt("hbase.master.assignment.timeoutmonitor.timeout", 1800000), serverManager);
        this.zkTable = new ZKTable(this.master.getZooKeeper());
        this.maximumAssignmentAttempts = this.master.getConfiguration().getInt("hbase.assignment.maximum.attempts", 10);
        this.zooKeeperEventHandler = new ZooKeeperEventHandler(this.master.getZooKeeper());
    }

    void startTimeOutMonitor() {
        Threads.setDaemonThreadRunning(timeoutMonitor,
                master.getServerName() + ".timeoutMonitor");
    }

   /**
     * @return Instance of ZKTable.
     */
    public ZKTable getZKTable() {
        // These are 'expensive' to make involving trip to zk ensemble so allow
        // sharing.
        return this.zkTable;
    }

    /**
     * Reset all unassigned znodes.  Called on startup of master.
     * Call {@link #assignAllUserRegions()} after root and meta have been assigned.
     * @throws IOException
     * @throws KeeperException
     */
    void cleanoutUnassigned() throws IOException, KeeperException {
        // Cleanup any existing ZK nodes and start watching
        ZKAssign.deleteAllNodes(watcher);
        ZKUtil.listChildrenAndWatchForNewChildren(this.watcher,
                this.watcher.assignmentZNode);
    }


    /**
     * Waits until the specified region has completed assignment.
     * <p>
     * If the region is already assigned, returns immediately.  Otherwise, method
     * blocks until the region is assigned.
     * @param regionInfo region to wait on assignment for
     * @throws InterruptedException
     */
    public void waitForAssignment(HRegionInfo regionInfo)
            throws InterruptedException {
        synchronized(regions) {
            while(!regions.containsKey(regionInfo)) {
                regions.wait();
            }
        }
    }

    public void stop() {
        this.timeoutMonitor.interrupt();
    }

    /**
     * Check whether the RegionServer is online.
     */
    public boolean isServerOnline(String serverName) {
        return this.serverManager.isServerOnline(serverName);
    }


    /**
     * Process shutdown server removing any assignments.
     *
     * @param hsi Server that went down.
     * @return list of regions in transition and region plans on this server
     */
    public RegionsOnDeadServer processServerShutdown(final HServerInfo hsi) {
        RegionsOnDeadServer regionsOnDeadServer = new RegionsOnDeadServer();
        Set<HRegionInfo> regionsFromRegionPlansForServer = new HashSet<HRegionInfo>();
        // Clean out any existing assignment plans for this server
        synchronized (this.regionPlans) {
            for (Iterator <Map.Entry<String, RegionPlan>> i =
                 this.regionPlans.entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, RegionPlan> e = i.next();
                HServerInfo otherHsi = e.getValue().getDestination();
                // The HSI will be null if the region is planned for a random assign.
                if (otherHsi != null && otherHsi.equals(hsi)) {
                    // Store the related regions in regionPlans.
                    regionsFromRegionPlansForServer.add(e.getValue().getRegionInfo());
                    // Use iterator's remove else we'll get CME
                    i.remove();
                }
            }
        }

        regionsOnDeadServer
                .setRegionsFromRegionPlansForServer(regionsFromRegionPlansForServer);
        // TODO: Do we want to sync on RIT here?
        // Remove this server from map of servers to regions, and remove all regions
        // of this server from online map of regions.
        Set<HRegionInfo> deadRegions = null;
        List<RegionState> rits = new ArrayList<RegionState>();
        synchronized (this.regions) {
            Set<HRegionInfo> assignedRegions = this.servers.remove(hsi);
            if (assignedRegions == null || assignedRegions.isEmpty()) {
                regionsOnDeadServer.setRegionsInTransition(rits);
                // No regions on this server, we are done, return empty list of RITs
                return regionsOnDeadServer;
            }
            deadRegions = new TreeSet<HRegionInfo>(assignedRegions);
            for (HRegionInfo region : deadRegions) {
                this.regions.remove(region);
            }
        }
        // See if any of the regions that were online on this server were in RIT
        // If they are, normal timeouts will deal with them appropriately so
        // let's skip a manual re-assignment.
        synchronized (regionsInTransition) {
            for (RegionState region : this.regionsInTransition.values()) {
                if (deadRegions.remove(region.getRegion())) {
                    rits.add(region);
                }
            }
        }
        regionsOnDeadServer.setRegionsInTransition(rits);
        return regionsOnDeadServer;
    }
    /**
     * Update inmemory structures.
     * @param hsi Server that reported the split
     * @param parent Parent region that was split
     * @param a Daughter region A
     * @param b Daughter region B
     */
    public void handleSplitReport(final HServerInfo hsi, final HRegionInfo parent,
                                  final HRegionInfo a, final HRegionInfo b) {
        regionOffline(parent);
        // Remove any CLOSING node, if exists, due to race between master & rs
        // for close & split.  Not putting into regionOffline method because it is
        // called from various locations.
        try {
            RegionTransitionData node = ZKAssign.getDataNoWatch(this.watcher,
                    parent.getEncodedName(), null);
            if (node != null) {
                if (node.getEventType().equals(EventType.RS_ZK_REGION_CLOSING)) {
                    ZKAssign.deleteClosingNode(this.watcher, parent);
                } else {
                    LOG.warn("Split report has RIT node (shouldnt have one): " +
                            parent + " node: " + node);
                }
            }
        } catch (KeeperException e) {
            LOG.warn("Exception while validating RIT during split report", e);
        }
        synchronized (this.regions) {
            //one daughter is already online, do nothing
            HServerInfo hsia = this.regions.get(a);
            if (hsia != null){
                LOG.warn("Trying to process the split of " +a.getEncodedName()+ ", " +
                        "but it was already done and one daughter is on region server " + hsia);
                return;
            }
        }

        regionOnline(a, hsi);
        regionOnline(b, hsi);

        // There's a possibility that the region was splitting while a user asked
        // the master to disable, we need to make sure we close those regions in
        // that case. This is not racing with the region server itself since RS
        // report is done after the split transaction completed.
        if (this.zkTable.isDisablingOrDisabledTable(
                parent.getTableDesc().getNameAsString())) {
            unassign(a);
            unassign(b);
        }
    }

    /**
     * @return A clone of current assignments. Note, this is assignments only.
     * If a new server has come in and it has no regions, it will not be included
     * in the returned Map.
     */
    Map<HServerInfo, List<HRegionInfo>> getAssignments() {
        // This is an EXPENSIVE clone.  Cloning though is the safest thing to do.
        // Can't let out original since it can change and at least the loadbalancer
        // wants to iterate this exported list.  We need to synchronize on regions
        // since all access to this.servers is under a lock on this.regions.
        Map<HServerInfo, List<HRegionInfo>> result = null;
        synchronized (this.regions) {
            result = new HashMap<HServerInfo, List<HRegionInfo>>(this.servers.size());
            for (Map.Entry<HServerInfo, Set<HRegionInfo>> e: this.servers.entrySet()) {
                List<HRegionInfo> shallowCopy = new ArrayList<HRegionInfo>(e.getValue());
                HServerInfo clone = new HServerInfo(e.getKey());
                // Set into server load the number of regions this server is carrying
                // The load balancer calculation needs it at least and its handy.
                clone.getLoad().setNumberOfRegions(e.getValue().size());
                result.put(clone, shallowCopy);
            }
        }
        return result;
    }

     /**
     * @return A clone of current assignments. Note, this is assignments only.
     * If a new server has come in and it has no regions, it will not be included
     * in the returned Map.
     */
    Map<HServerInfo, List<HRegionInfo>> getAssignments() {
        // This is an EXPENSIVE clone.  Cloning though is the safest thing to do.
        // Can't let out original since it can change and at least the loadbalancer
        // wants to iterate this exported list.  We need to synchronize on regions
        // since all access to this.servers is under a lock on this.regions.
        Map<HServerInfo, List<HRegionInfo>> result = null;
        synchronized (this.regions) {
            result = new HashMap<HServerInfo, List<HRegionInfo>>(this.servers.size());
            for (Map.Entry<HServerInfo, Set<HRegionInfo>> e: this.servers.entrySet()) {
                List<HRegionInfo> shallowCopy = new ArrayList<HRegionInfo>(e.getValue());
                HServerInfo clone = new HServerInfo(e.getKey());
                // Set into server load the number of regions this server is carrying
                // The load balancer calculation needs it at least and its handy.
                clone.getLoad().setNumberOfRegions(e.getValue().size());
                result.put(clone, shallowCopy);
            }
        }
        return result;
    }

    /**
     * @param plan Plan to execute.
     */
    void balance(final RegionPlan plan) {
        synchronized (this.regionPlans) {
            this.regionPlans.put(plan.getRegionName(), plan);
        }
        unassign(plan.getRegionInfo());
    }

}
static class CreateUnassignedAsyncCallback implements AsyncCallback.StringCallback {
    private final Log LOG = LogFactory.getLog(CreateUnassignedAsyncCallback.class);
    private final ZooKeeperWatcher zkw;
    private final HServerInfo destination;
    private final AtomicInteger counter;

    CreateUnassignedAsyncCallback(final ZooKeeperWatcher zkw, final HServerInfo destination, final AtomicInteger counter) {
        this.zkw = zkw;
        this.destination = destination;
        this.counter = counter;
    }

    @Override
        public void processResult(int rc, String path, Object ctx, String name) {
            if (rc != 0) {
                // Thisis resultcode.  If non-zero, need to resubmit.
                LOG.warn("rc != 0 for " + path + " -- retryable connectionloss -- " +
                        "FIX see http://wiki.apache.org/hadoop/ZooKeeper/FAQ#A2");
                this.zkw.abort("Connectionloss writing unassigned at " + path +
                        ", rc=" + rc, null);
                return;
            }
            LOG.debug("rs=" + (RegionState)ctx + ", server=" + this.destination.getServerName());
            // Async exists to set a watcher so we'll get triggered when
            // unassigned node changes.
            this.zkw.getZooKeeper().exists(path, this.zkw,
                    new ExistsUnassignedAsyncCallback(this.counter), ctx);
        }
    }
}
static class ExistsUnassignedAsyncCallback implements AsyncCallback.StatCallback {
    private final Log LOG = LogFactory.getLog(ExistsUnassignedAsyncCallback.class);
    private final AtomicInteger counter;

    ExistsUnassignedAsyncCallback(final AtomicInteger counter) {
        this.counter = counter;
    }

    @Override
        public void processResult(int rc, String path, Object ctx, Stat stat) {
            if (rc != 0) {
                // Thisis resultcode.  If non-zero, need to resubmit.
                LOG.warn("rc != 0 for " + path + " -- retryable connectionloss -- " +
                        "FIX see http://wiki.apache.org/hadoop/ZooKeeper/FAQ#A2");
                return;
            }
            RegionState state = (RegionState)ctx;
            LOG.debug("rs=" + state);
            // Transition RegionState to PENDING_OPEN here in master; means we've
            // sent the open.  We're a little ahead of ourselves here since we've not
            // yet sent out the actual open but putting this state change after the
            // call to open risks our writing PENDING_OPEN after state has been moved
            // to OPENING by the regionserver.
            state.update(RegionState.State.PENDING_OPEN);
            this.counter.addAndGet(1);
        }
    }
}
static class StartupBulkAssigner extends BulkAssigner {
    final Map<HServerInfo, List<HRegionInfo>> bulkPlan;
    final AssignmentManager assignmentManager;

    StartupBulkAssigner(final Server server, final Map<HServerInfo, List<HRegionInfo>> bulkPlan, final AssignmentManager am) {
        super(server);
        this.bulkPlan = bulkPlan;
        this.assignmentManager = am;
    }

    @Override
        public boolean bulkAssign(boolean sync) throws InterruptedException {
            // Disable timing out regions in transition up in zk while bulk assigning.
            this.assignmentManager.timeoutMonitor.bulkAssign(true);
            try {
                return super.bulkAssign(sync);
            } finally {
                // Reenable timing out regions in transition up in zi.
                this.assignmentManager.timeoutMonitor.bulkAssign(false);
            }
        }


     @Override
        protected String getThreadNamePrefix() {
            return this.server.getServerName() + "-StartupBulkAssigner";
        }



        @Override
        protected void populatePool(java.util.concurrent.ExecutorService pool) {
            for (Map.Entry<HServerInfo, List<HRegionInfo>> e: this.bulkPlan.entrySet()) {
                pool.execute(new SingleServerBulkAssigner(e.getKey(), e.getValue(),
                        this.assignmentManager, true));
            }
        }

    protected boolean waitUntilDone(final long timeout)
                throws InterruptedException {
            Set<HRegionInfo> regionSet = new HashSet<HRegionInfo>();
            for (List<HRegionInfo> regionList : bulkPlan.values()) {
                regionSet.addAll(regionList);
            }
            return this.assignmentManager.waitUntilNoRegionsInTransition(timeout, regionSet);
        }

    @Override
        protected long getTimeoutOnRIT() {
            // Guess timeout.  Multiply the number of regions on a random server
            // by how long we thing one region takes opening.
            long perRegionOpenTimeGuesstimate =
                    this.server.getConfiguration().getLong("hbase.bulk.assignment.perregion.open.time", 1000);
            int regionsPerServer =
                    this.bulkPlan.entrySet().iterator().next().getValue().size();
            long timeout = perRegionOpenTimeGuesstimate * regionsPerServer;
            LOG.debug("Timeout-on-RIT=" + timeout);
            return timeout;
        }
}
static class GeneralBulkAssigner extends StartupBulkAssigner {
    GeneralBulkAssigner(final Server server, final Map<HServerInfo, List<HRegionInfo>> bulkPlan, final AssignmentManager am) {
        super(server, bulkPlan, am);
    }

   @Override
        protected UncaughtExceptionHandler getUncaughtExceptionHandler() {
            return new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    LOG.warn("Assigning regions in " + t.getName(), e);
                }
            };
        }
    }
}
static class SingleServerBulkAssigner implements Runnable {
    private final HServerInfo regionserver;
    private final List<HRegionInfo> regions;
    private final AssignmentManager assignmentManager;

    SingleServerBulkAssigner(final HServerInfo regionserver, final List<HRegionInfo> regions, final AssignmentManager am, final boolean startUp) {
        this.regionserver = regionserver;
        this.regions = regions;
        this.assignmentManager = am;
    }

  @Override
        public void run() {
            this.assignmentManager.assign(this.regionserver, this.regions);
        }
}
public class TimeoutMonitor extends Chore {
    private final int timeout;
    private boolean bulkAssign = false;
    private boolean allRegionServersOffline = false;
    private ServerManager serverManager;

    public TimeoutMonitor(final int period, final Stoppable stopper, final int timeout, final ServerManager serverManager) {
        super("AssignmentTimeoutMonitor", period, stopper);
        this.timeout = timeout;
        this.serverManager = serverManager;
    }

    /**
         * @param bulkAssign If true, we'll suspend checking regions in transition
         * up in zookeeper.  If false, will reenable check.
         * @return Old setting for bulkAssign.
         */
        public boolean bulkAssign(final boolean bulkAssign) {
            boolean result = this.bulkAssign;
            this.bulkAssign = bulkAssign;
            return result;
        }

     private synchronized void setAllRegionServersOffline(
                boolean allRegionServersOffline) {
            this.allRegionServersOffline = allRegionServersOffline;
        }


    @Override
        protected void chore() {
            // If bulkAssign in progress, suspend checks
            if (this.bulkAssign) return;
            boolean allRSsOffline = this.serverManager.getOnlineServersList()
                    .isEmpty();
            List<HRegionInfo> unassigns = new ArrayList<HRegionInfo>();
            Map<HRegionInfo, Boolean> assigns =
                    new HashMap<HRegionInfo, Boolean>();
            synchronized (regionsInTransition) {
                // Iterate all regions in transition checking for time outs
                long now = System.currentTimeMillis();
                for (RegionState regionState : regionsInTransition.values()) {
                    HRegionInfo regionInfo = regionState.getRegion();
                    if (regionState.getStamp() + timeout <= now) {
                        actOnTimeOut(unassigns, assigns, regionState, regionInfo);
                    }
                    else if(this.allRegionServersOffline && !allRSsOffline){
                        actOnTimeOut(unassigns, assigns, regionState, regionInfo);
                    }
                }
            }
            setAllRegionServersOffline(allRSsOffline);
            // Finish the work for regions in PENDING_CLOSE state
            for (HRegionInfo hri: unassigns) {
                unassign(hri, true);
            }
            for (Map.Entry<HRegionInfo, Boolean> e: assigns.entrySet()){
                assign(e.getKey(), false, e.getValue());
            }
        }

    private void actOnTimeOut(List<HRegionInfo> unassigns,
                                  Map<HRegionInfo, Boolean> assigns, RegionState regionState,
                                  HRegionInfo regionInfo) {
            LOG.info("Regions in transition timed out:  " + regionState);
            // Expired!  Do a retry.
            switch (regionState.getState()) {
                case CLOSED:
                    LOG.info("Region " + regionInfo.getEncodedName() +
                            " has been CLOSED for too long, waiting on queued " +
                            "ClosedRegionHandler to run or server shutdown");
                    // Update our timestamp.
                    synchronized(regionState) {
                        regionState.update(regionState.getState());
                    }
                    break;
                case OFFLINE:
                    LOG.info("Region has been OFFLINE for too long, " +
                            "reassigning " + regionInfo.getRegionNameAsString() +
                            " to a random server");
                    assigns.put(regionState.getRegion(), Boolean.FALSE);
                    break;
                case PENDING_OPEN:
                    LOG.info("Region has been PENDING_OPEN for too " +
                            "long, reassigning region=" +
                            regionInfo.getRegionNameAsString());
                    assigns.put(regionState.getRegion(), Boolean.TRUE);
                    break;
                case OPENING:
                    LOG.info("Region has been OPENING for too " +
                            "long, reassigning region=" +
                            regionInfo.getRegionNameAsString());
                    // Should have a ZK node in OPENING state
                    try {
                        String node = ZKAssign.getNodeName(watcher,
                                regionInfo.getEncodedName());
                        Stat stat = new Stat();
                        RegionTransitionData data = ZKAssign.getDataNoWatch(watcher,
                                node, stat);
                        if (data == null) {
                            LOG.warn("Data is null, node " + node + " no longer exists");
                            break;
                        }
                        if (data.getEventType() == EventType.RS_ZK_REGION_OPENED) {
                            LOG.debug("Region has transitioned to OPENED, allowing " +
                                    "watched event handlers to process");
                            break;
                        } else if (data.getEventType() != EventType.RS_ZK_REGION_OPENING
                                && data.getEventType() != EventType.RS_ZK_REGION_FAILED_OPEN) {
                            LOG.warn("While timing out a region in state OPENING, " +
                                    "found ZK node in unexpected state: " +
                                    data.getEventType());
                            break;
                        }
                        // Attempt to transition node into OFFLINE
                        try {
                            data = new RegionTransitionData(
                                    EventType.M_ZK_REGION_OFFLINE, regionInfo.getRegionName(),
                                    master.getServerName());
                            if (ZKUtil.setData(watcher, node, data.getBytes(),
                                    stat.getVersion())) {
                                // Node is now OFFLINE, let's trigger another assignment
                                ZKUtil.getDataAndWatch(watcher, node); // re-set the watch
                                LOG.info("Successfully transitioned region=" +
                                        regionInfo.getRegionNameAsString() + " into OFFLINE" +
                                        " and forcing a new assignment");
                                assigns.put(regionState.getRegion(), Boolean.TRUE);
                            }
                        } catch (KeeperException.NoNodeException nne) {
                            // Node did not exist, can't time this out
                        }
                    } catch (KeeperException ke) {
                        LOG.error("Unexpected ZK exception timing out CLOSING region",
                                ke);
                        break;
                    }
                    break;
                case OPEN:
                    LOG.error("Region has been OPEN for too long, " +
                            "we don't know where region was opened so can't do anything");
                    synchronized(regionState) {
                        regionState.update(regionState.getState());
                    }
                    break;

                case PENDING_CLOSE:
                    LOG.info("Region has been PENDING_CLOSE for too " +
                            "long, running forced unassign again on region=" +
                            regionInfo.getRegionNameAsString());
                    try {
                        // If the server got the RPC, it will transition the node
                        // to CLOSING, so only do something here if no node exists
                        if (!ZKUtil.watchAndCheckExists(watcher,
                                ZKAssign.getNodeName(watcher, regionInfo.getEncodedName()))) {
                            // Queue running of an unassign -- do actual unassign
                            // outside of the regionsInTransition lock.
                            unassigns.add(regionInfo);
                        }
                    } catch (NoNodeException e) {
                        LOG.debug("Node no longer existed so not forcing another " +
                                "unassignment");
                    } catch (KeeperException e) {
                        LOG.warn("Unexpected ZK exception timing out a region " +
                                "close", e);
                    }
                    break;
                case CLOSING:
                    LOG.info("Region has been CLOSING for too " +
                            "long, this should eventually complete or the server will " +
                            "expire, doing nothing");
                    break;
            }
        }
    }
}
public static class RegionState implements Writable {
    private HRegionInfo region;

    public enum State {
        OFFLINE,        // region is in an offline state
        PENDING_OPEN,   // sent rpc to server to open but has not begun
        OPENING,        // server has begun to open but not yet done
        OPEN,           // server opened region and updated meta
        PENDING_CLOSE,  // sent rpc to server to close but has not begun
        CLOSING,        // server has begun to close but not yet done
        CLOSED          // server closed region and updated meta
    }

    private State state;
    private long stamp;

    public RegionState() {}

    RegionState(HRegionInfo region, State state) {
        this(region, state, System.currentTimeMillis());
    }

    RegionState(HRegionInfo region, State state, long stamp) {
        this.region = region;
        this.state = state;
        this.stamp = stamp;
    }

    public void update(State state, long stamp) {
            this.state = state;
            this.stamp = stamp;
        }

        public void update(State state) {
            this.state = state;
            this.stamp = System.currentTimeMillis();
        }

        public State getState() {
            return state;
        }

        public long getStamp() {
            return stamp;
        }

        public HRegionInfo getRegion() {
            return region;
        }

        public boolean isClosing() {
            return state == State.CLOSING;
        }

        public boolean isClosed() {
            return state == State.CLOSED;
        }

        public boolean isPendingClose() {
            return state == State.PENDING_CLOSE;
        }

        public boolean isOpening() {
            return state == State.OPENING;
        }

        public boolean isOpened() {
            return state == State.OPEN;
        }

        public boolean isPendingOpen() {
            return state == State.PENDING_OPEN;
        }

        public boolean isOffline() {
            return state == State.OFFLINE;
        }

        @Override
        public String toString() {
            return region.getRegionNameAsString() + " state=" + state +
                    ", ts=" + stamp;
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            region = new HRegionInfo();
            region.readFields(in);
            state = State.valueOf(in.readUTF());
            stamp = in.readLong();
        }

        @Override
        public void write(DataOutput out) throws IOException {
            region.write(out);
            out.writeUTF(state.name());
            out.writeLong(stamp);
        }
    }
}

public static class RegionsOnDeadServer {
    private Set<HRegionInfo> regionsFromRegionPlansForServer = null;
    private List<RegionState> regionsInTransition = null;

    public Set<HRegionInfo> getRegionsFromRegionPlansForServer() {
            return regionsFromRegionPlansForServer;
        }

        public void setRegionsFromRegionPlansForServer(
                Set<HRegionInfo> regionsFromRegionPlansForServer) {
            this.regionsFromRegionPlansForServer = regionsFromRegionPlansForServer;
        }

        public List<RegionState> getRegionsInTransition() {
            return regionsInTransition;
        }

        public void setRegionsInTransition(List<RegionState> regionsInTransition) {
            this.regionsInTransition = regionsInTransition;
        }
    }
}
/**
     * Touch timers for all regions in transition that have the passed
     * <code>hsi</code> in common.
     * Call this method whenever a server checks in.  Doing so helps the case where
     * a new regionserver has joined the cluster and its been given 1k regions to
     * open.  If this method is tickled every time the region reports in a
     * successful open then the 1k-th region won't be timed out just because its
     * sitting behind the open of 999 other regions.  This method is NOT used
     * as part of bulk assign -- there we have a different mechanism for extending
     * the regions in transition timer (we turn it off temporarily -- because
     * there is no regionplan involved when bulk assigning.
     * @param hsi
     */
    private void updateTimers(final HServerInfo hsi) {
        // This loop could be expensive.
        // First make a copy of current regionPlan rather than hold sync while
        // looping because holding sync can cause deadlock.  Its ok in this loop
        // if the Map we're going against is a little stale
        Map<String, RegionPlan> copy = new HashMap<String, RegionPlan>();
        synchronized(this.regionPlans) {
            copy.putAll(this.regionPlans);
        }
        for (Map.Entry<String, RegionPlan> e: copy.entrySet()) {
            if (!e.getValue().getDestination().equals(hsi)) continue;
            RegionState rs = null;
            synchronized (this.regionsInTransition) {
                rs = this.regionsInTransition.get(e.getKey());
            }
            if (rs == null) continue;
            synchronized (rs) {
                rs.update(rs.getState());
            }
        }
    }


/**
     * @param regionInfo
     * @param deadServers Map of deadServers and the regions they were carrying;
     * can be null.
     * @return True if the passed regionInfo in the passed map of deadServers?
     */
    private boolean isOnDeadServer(final HRegionInfo regionInfo,
                                   final Map<String, List<Pair<HRegionInfo, Result>>> deadServers) {
        if (deadServers == null) return false;
        for (Map.Entry<String, List<Pair<HRegionInfo, Result>>> deadServer:
                deadServers.entrySet()) {
            for (Pair<HRegionInfo, Result> e: deadServer.getValue()) {
                if (e.getFirst().equals(regionInfo)) return true;
            }
        }
        return false;
    }

 /**
     * Handles various states an unassigned node can be in.
     * <p>
     * Method is called when a state change is suspected for an unassigned node.
     * <p>
     * This deals with skipped transitions (we got a CLOSED but didn't see CLOSING
     * yet).
     * @param data
     * @param expectedVersion
     */
    private void handleRegion(final RegionTransitionData data, int expectedVersion) {
        synchronized(regionsInTransition) {
            if (data == null || data.getServerName() == null) {
                LOG.warn("Unexpected NULL input " + data);
                return;
            }
            // Check if this is a special HBCK transition
            if (data.getServerName().equals(HConstants.HBCK_CODE_NAME)) {
                handleHBCK(data);
                return;
            }
            // Verify this is a known server
            if (!serverManager.isServerOnline(data.getServerName()) &&
                    !this.master.getServerName().equals(data.getServerName())
                    && !ignoreStatesRSOffline.contains(data.getEventType())) {
                LOG.warn("Attempted to handle region transition for server but " +
                        "server is not online: " + Bytes.toString(data.getRegionName()));
                return;
            }
            String encodedName = HRegionInfo.encodeRegionName(data.getRegionName());
            String prettyPrintedRegionName = HRegionInfo.prettyPrint(encodedName);
            // Printing if the event was created a long time ago helps debugging
            boolean lateEvent = data.getStamp() <
                    (System.currentTimeMillis() - 15000);
            LOG.debug("Handling transition=" + data.getEventType() +
                    ", server=" + data.getServerName() + ", region=" +
                    prettyPrintedRegionName +
                    (lateEvent? ", which is more than 15 seconds late" : ""));
            RegionState regionState = regionsInTransition.get(encodedName);
            switch (data.getEventType()) {
                case M_ZK_REGION_OFFLINE:
                    // Nothing to do.
                    break;

                case RS_ZK_REGION_CLOSING:
                    // Should see CLOSING after we have asked it to CLOSE or additional
                    // times after already being in state of CLOSING
                    if (regionState == null ||
                            (!regionState.isPendingClose() && !regionState.isClosing())) {
                        LOG.warn("Received CLOSING for region " + prettyPrintedRegionName +
                                " from server " + data.getServerName() + " but region was in " +
                                " the state " + regionState + " and not " +
                                "in expected PENDING_CLOSE or CLOSING states");
                        return;
                    }
                    // Transition to CLOSING (or update stamp if already CLOSING)
                    regionState.update(RegionState.State.CLOSING, data.getStamp());
                    break;

                case RS_ZK_REGION_CLOSED:
                    // Should see CLOSED after CLOSING but possible after PENDING_CLOSE
                    if (regionState == null ||
                            (!regionState.isPendingClose() && !regionState.isClosing())) {
                        LOG.warn("Received CLOSED for region " + prettyPrintedRegionName +
                                " from server " + data.getServerName() + " but region was in " +
                                " the state " + regionState + " and not " +
                                "in expected PENDING_CLOSE or CLOSING states");
                        return;
                    }
                    // Handle CLOSED by assigning elsewhere or stopping if a disable
                    // If we got here all is good.  Need to update RegionState -- else
                    // what follows will fail because not in expected state.
                    regionState.update(RegionState.State.CLOSED, data.getStamp());
                    this.executorService.submit(new ClosedRegionHandler(master,
                            this, regionState.getRegion()));
                    break;

                case RS_ZK_REGION_OPENING:
                    // Should see OPENING after we have asked it to OPEN or additional
                    // times after already being in state of OPENING
                    if (regionState == null ||
                            (!regionState.isPendingOpen() && !regionState.isOpening())) {
                        LOG.warn("Received OPENING for region " +
                                prettyPrintedRegionName +
                                " from server " + data.getServerName() + " but region was in " +
                                " the state " + regionState + " and not " +
                                "in expected PENDING_OPEN or OPENING states");
                        return;
                    }
                    // Transition to OPENING (or update stamp if already OPENING)
                    regionState.update(RegionState.State.OPENING, data.getStamp());
                    break;
                case RS_ZK_REGION_FAILED_OPEN:
                    if (regionState == null
                            || (!regionState.isPendingOpen() && !regionState.isOpening())) {
                        LOG.warn("Received FAILED_OPEN for region " + prettyPrintedRegionName
                                + " from server " + data.getServerName() + " but region was in "
                                + " the state " + regionState
                                + " and not in PENDING_OPEN or OPENING");
                        return;
                    }
                    // Handle this the same as if it were opened and then closed.
                    regionState.update(RegionState.State.CLOSED, data.getStamp());
                    this.executorService.submit(new ClosedRegionHandler(master, this,
                            regionState.getRegion()));
                    break;

                case RS_ZK_REGION_OPENED:
                    // Should see OPENED after OPENING but possible after PENDING_OPEN
                    if (regionState == null ||
                            (!regionState.isPendingOpen() && !regionState.isOpening())) {
                        LOG.warn("Received OPENED for region " +
                                prettyPrintedRegionName +
                                " from server " + data.getServerName() + " but region was in " +
                                " the state " + regionState + " and not " +
                                "in expected PENDING_OPEN or OPENING states");
                        return;
                    }
                    // Handle OPENED by removing from transition and deleted zk node
                    regionState.update(RegionState.State.OPEN, data.getStamp());
                    this.executorService.submit(
                            new OpenedRegionHandler(master, this, regionState.getRegion(),
                                    this.serverManager.getServerInfo(
                                            data.getServerName()), expectedVersion));
                    break;
            }
        }
    }


/**
     * Handle a ZK unassigned node transition triggered by HBCK repair tool.
     * <p>
     * This is handled in a separate code path because it breaks the normal rules.
     * @param data
     */
    private void handleHBCK(RegionTransitionData data) {
        String encodedName = HRegionInfo.encodeRegionName(data.getRegionName());
        LOG.info("Handling HBCK triggered transition=" + data.getEventType() +
                ", server=" + data.getServerName() + ", region=" +
                HRegionInfo.prettyPrint(encodedName));
        RegionState regionState = regionsInTransition.get(encodedName);
        switch (data.getEventType()) {
            case M_ZK_REGION_OFFLINE:
                HRegionInfo regionInfo = null;
                if (regionState != null) {
                    regionInfo = regionState.getRegion();
                } else {
                    try {
                        byte[] name = data.getRegionName();
                        Pair<HRegionInfo, HServerAddress> p = MetaReader.getRegion(catalogTracker, name);
                        regionInfo = p.getFirst();
                    } catch (IOException e) {
                        LOG.info("Exception reading META doing HBCK repair operation", e);
                        return;
                    }
                }
                LOG.info("HBCK repair is triggering assignment of region=" +
                        regionInfo.getRegionNameAsString());
                // trigger assign, node is already in OFFLINE so don't need to update ZK
                assign(regionInfo, false);
                break;

            default:
                LOG.warn("Received unexpected region state from HBCK (" +
                        data.getEventType() + ")");
                break;
        }
    }


 /**
     * Rebuild the list of user regions and assignment information.
     * <p>
     * Returns a map of servers that are not found to be online and the regions
     * they were hosting.
     * @return map of servers not online to their assigned regions, as stored
     *         in META
     * @throws IOException
     * @throws KeeperException
     */
    private Map<String, List<Pair<HRegionInfo,Result>>> rebuildUserRegions()
            throws IOException, KeeperException {
        // Region assignment from META
        List<Result> results = MetaReader.fullScanOfResults(catalogTracker);
        // Map of offline servers and their regions to be returned
        Map<String, List<Pair<HRegionInfo,Result>>> offlineServers =
                new TreeMap<String, List<Pair<HRegionInfo,Result>>>();
        // store all the disabling state table names
        Set<String> disablingTables = new HashSet<String>(1);
        // Iterate regions in META
        for (Result result : results) {
            Pair<HRegionInfo,HServerInfo> region =
                    MetaReader.metaRowToRegionPairWithInfo(result);
            if (region == null) continue;
            HServerInfo regionLocation = region.getSecond();
            HRegionInfo regionInfo = region.getFirst();
            String disablingTableName = regionInfo.getTableDesc().getNameAsString();
            if (regionInfo.isOffline() && regionInfo.isSplit()) continue;

            if (regionLocation == null) {
                // Region not being served, add to region map with no assignment
                // If this needs to be assigned out, it will also be in ZK as RIT
                // add if the table is not in disabled state
                if (false == checkIfRegionBelongsToDisabled(regionInfo)) {
                    this.regions.put(regionInfo, null);
                }
                if (checkIfRegionBelongsToDisabling(regionInfo)) {
                    disablingTables.add(disablingTableName);
                }
            } else if (!serverManager.isServerOnline(regionLocation.getServerName())) {
                // Region is located on a server that isn't online
                List<Pair<HRegionInfo,Result>> offlineRegions =
                        offlineServers.get(regionLocation.getServerName());
                if (offlineRegions == null) {
                    offlineRegions = new ArrayList<Pair<HRegionInfo,Result>>(1);
                    offlineServers.put(regionLocation.getServerName(), offlineRegions);
                }
                offlineRegions.add(new Pair<HRegionInfo,Result>(regionInfo, result));
            } else {
                // Region is being served and on an active server
                // add only if region not in disabled table
                if (false == checkIfRegionBelongsToDisabled(regionInfo)) {
                    regions.put(regionInfo, regionLocation);
                    addToServers(regionLocation, regionInfo);
                }
                if (checkIfRegionBelongsToDisabling(regionInfo)) {
                    disablingTables.add(disablingTableName);
                }
            }
        }
        // Recover the tables that were not fully moved to DISABLED state.
        // These tables are in DISABLING state when the master
        // restarted/switched.
        if (disablingTables.size() != 0) {
            // Create a watcher on the zookeeper node
            ZKUtil.listChildrenAndWatchForNewChildren(watcher,
                    watcher.assignmentZNode);
            for (String tableName : disablingTables) {
                // Recover by calling DisableTableHandler
                LOG.info("The table " + tableName
                        + " is in DISABLING state.  Hence recovering by moving the table"
                        + " to DISABLED state.");
                new DisableTableHandler(this.master, tableName.getBytes(),
                        catalogTracker, this).process();
            }
        }
        return offlineServers;
    }
 private boolean checkIfRegionBelongsToDisabled(HRegionInfo regionInfo) {
        String tableName = regionInfo.getTableDesc().getNameAsString();
        return getZKTable().isDisabledTable(tableName);
    }

    private boolean checkIfRegionBelongsToDisabling(HRegionInfo regionInfo) {
        String tableName = regionInfo.getTableDesc().getNameAsString();
        return getZKTable().isDisablingTable(tableName);
    }


/**
     * Processes list of dead servers from result of META scan.
     * <p>
     * This is used as part of failover to handle RegionServers which failed
     * while there was no active master.
     * <p>
     * Method stubs in-memory data to be as expected by the normal server shutdown
     * handler.
     *
     * @param deadServers
     * @throws IOException
     * @throws KeeperException
     */
    private void processDeadServers(
            Map<String, List<Pair<HRegionInfo, Result>>> deadServers)
            throws IOException, KeeperException {
        for (Map.Entry<String, List<Pair<HRegionInfo,Result>>> deadServer :
                deadServers.entrySet()) {
            List<Pair<HRegionInfo,Result>> regions = deadServer.getValue();
            for (Pair<HRegionInfo,Result> region : regions) {
                HRegionInfo regionInfo = region.getFirst();
                Result result = region.getSecond();
                // If region was in transition (was in zk) force it offline for reassign
                try {
                    //Process with existing RS shutdown code
                    boolean assign =
                            ServerShutdownHandler.processDeadRegion(regionInfo, result, this,
                                    this.catalogTracker);
                    RegionTransitionData data = ZKAssign.getData(watcher, regionInfo.getEncodedName());

                    //If zk node of this region has been updated by a live server,
                    //we consider that this region is being handled.
                    //So we should skip it and process it in processRegionsInTransition.
                    if (data != null && data.getServerName() != null &&
                            serverManager.isServerOnline(data.getServerName())){
                        LOG.info("The region " + regionInfo.getEncodedName() +
                                "is being handled on " + data.getServerName());
                        continue;
                    }
                    if (assign) {
                        ZKAssign.createOrForceNodeOffline(watcher, regionInfo,
                                master.getServerName());
                    }
                } catch (KeeperException.NoNodeException nne) {
                    // This is fine
                }
            }
        }
    }


 /*
     * Presumes caller has taken care of necessary locking modifying servers Map.
     * @param hsi
     * @param hri
     */
    private void addToServers(final HServerInfo hsi, final HRegionInfo hri) {
        Set<HRegionInfo> hris = servers.get(hsi);
        if (hris == null) {
            hris = new ConcurrentSkipListSet<HRegionInfo>();
            servers.put(hsi, hris);
        }
        if (!hris.contains(hri)) hris.add(hri);
    }



