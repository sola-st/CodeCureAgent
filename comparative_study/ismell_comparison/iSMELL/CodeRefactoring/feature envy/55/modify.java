public class RegionTransitionProcessor {
    private Map<String, RegionState> regionsInTransition;
    private Map<String, HRegionInfo> failoverProcessedRegions;
    private Map<ServerName, List<Pair<HRegionInfo, Result>>> deadServers;
    private ServerManager serverManager;

    public RegionTransitionProcessor(Map<String, RegionState> regionsInTransition,
                                     Map<String, HRegionInfo> failoverProcessedRegions,
                                     Map<ServerName, List<Pair<HRegionInfo, Result>>> deadServers,
                                     ServerManager serverManager) {
        this.regionsInTransition = regionsInTransition;
        this.failoverProcessedRegions = failoverProcessedRegions;
        this.deadServers = deadServers;
        this.serverManager = serverManager;
    }

    public void processRegion(HRegionInfo regionInfo, RegionTransitionData data, int expectedVersion) {
        String encodedRegionName = regionInfo.getEncodedName();
        synchronized (regionsInTransition) {
            if (regionAlreadyProcessed(encodedRegionName)) {
                return;
            }
            switch (data.getEventType()) {
                case M_ZK_REGION_CLOSING:
                    processClosing(regionInfo, data);
                    break;
                case RS_ZK_REGION_CLOSED:
                case RS_ZK_REGION_FAILED_OPEN:
                    processClosed(regionInfo, data);
                    break;
                case M_ZK_REGION_OFFLINE:
                    processOffline(regionInfo, data);
                    break;
                case RS_ZK_REGION_OPENING:
                    processOpening(regionInfo, data);
                    break;
                case RS_ZK_REGION_OPENED:
                    processOpened(regionInfo, data, expectedVersion);
                    break;
            }
        }
    }

    private boolean regionAlreadyProcessed(String encodedRegionName) {
        return regionsInTransition.containsKey(encodedRegionName) ||
                failoverProcessedRegions.containsKey(encodedRegionName);
    }

    private void processClosing(HRegionInfo regionInfo, RegionTransitionData data) {
        String encodedRegionName = regionInfo.getEncodedName();
        if (isOnDeadServer(regionInfo, deadServers) &&
                (data.getOrigin() == null || !serverManager.isServerOnline(data.getOrigin()))) {
            forceOffline(regionInfo, data);
        } else {
            regionsInTransition.put(encodedRegionName, new RegionState(
                    regionInfo, RegionState.State.CLOSING,
                    data.getStamp(), data.getOrigin()));
        }
        failoverProcessedRegions.put(encodedRegionName, regionInfo);
    }

    private void processClosed(HRegionInfo regionInfo, RegionTransitionData data) {
        addToRITandCallClose(regionInfo, RegionState.State.CLOSED, data);
        failoverProcessedRegions.put(regionInfo.getEncodedName(), regionInfo);
    }

    private void processOffline(HRegionInfo regionInfo, RegionTransitionData data) {
        String encodedRegionName = regionInfo.getEncodedName();
        if (isOnDeadServer(regionInfo, deadServers) &&
                (data.getOrigin() == null || !serverManager.isServerOnline(data.getOrigin()))) {
            addToRITandCallClose(regionInfo, RegionState.State.OFFLINE, data);
        } else if (data.getOrigin() != null && !serverManager.isServerOnline(data.getOrigin())) {
            addToRITandCallClose(regionInfo, RegionState.State.OFFLINE, data);
        } else {
            regionsInTransition.put(encodedRegionName, new RegionState(
                    regionInfo, RegionState.State.PENDING_OPEN, data.getStamp(), data.getOrigin()));
        }
        failoverProcessedRegions.put(encodedRegionName, regionInfo);
    }

    private void processOpening(HRegionInfo regionInfo, RegionTransitionData data) {
        String encodedRegionName = regionInfo.getEncodedName();
        regionsInTransition.put(encodedRegionName, new RegionState(
                regionInfo, RegionState.State.OPENING, data.getStamp(), data.getOrigin()));
        if (regionInfo.isMetaTable()) {
            processOpeningState(regionInfo);
        }
        failoverProcessedRegions.put(encodedRegionName, regionInfo);
    }

    private void processOpened(HRegionInfo regionInfo, RegionTransitionData data, int expectedVersion) {
        String encodedRegionName = regionInfo.getEncodedName();
        regionsInTransition.put(encodedRegionName, new RegionState(
                regionInfo, RegionState.State.OPEN, data.getStamp(), data.getOrigin()));
        ServerName sn = data.getOrigin() == null ? null : data.getOrigin();
        if (sn == null) {
            LOG.warn("Region in transition " + regionInfo.getEncodedName() +
                    " references a null server; letting RIT timeout so will be " +
                    "assigned elsewhere");
        } else if (!serverManager.isServerOnline(sn) &&
                (isOnDeadServer(regionInfo, deadServers) || regionInfo.isMetaRegion() || regionInfo.isRootRegion())) {
            forceOffline(regionInfo, data);
        } else {
            new OpenedRegionHandler(master, this, regionInfo, sn, expectedVersion).process();
        }
        failoverProcessedRegions.put(encodedRegionName, regionInfo);
    }

    private boolean isOnDeadServer(HRegionInfo regionInfo, Map<ServerName, List<Pair<HRegionInfo, Result>>> deadServers) {
        // Implement the logic to check if the region is on a dead server
        return false; // Placeholder return value
    }

    private void forceOffline(HRegionInfo regionInfo, RegionTransitionData data) {
        // Implement the logic to force a region offline
    }

    private void addToRITandCallClose(HRegionInfo regionInfo, RegionState.State state, RegionTransitionData data) {
        // Implement the logic to add a region to RIT and call close
    }

    private void processOpeningState(HRegionInfo regionInfo) {
        // Implement the logic to process opening state
    }
}

    // In the original method
    void processRegionsInTransition(final RegionTransitionData data,
                                    final HRegionInfo regionInfo,
                                    final Map<ServerName, List<Pair<HRegionInfo, Result>>> deadServers,
                                    int expectedVersion) throws KeeperException {
        LOG.info("Processing region " + regionInfo.getRegionNameAsString() + " in state " + data.getEventType());
        List<HRegionInfo> hris = this.enablingTables.get(regionInfo.getTableNameAsString());
        if (hris != null && !hris.isEmpty()) {
            hris.remove(regionInfo);
        }

        RegionTransitionProcessor processor = new RegionTransitionProcessor(regionsInTransition, failoverProcessedRegions, deadServers, serverManager);
        processor.processRegion(regionInfo, data, expectedVersion);
    }
