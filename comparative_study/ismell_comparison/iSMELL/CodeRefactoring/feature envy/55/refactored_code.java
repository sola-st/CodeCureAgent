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
        // Implementation for processing closing regions
        // If zk node of the region was updated by a live server skip this
        // region and just add it into RIT.
        if (isOnDeadServer(regionInfo, deadServers) &&
                (data.getOrigin() == null || !serverManager.isServerOnline(data.getOrigin()))) {
            // If was on dead server, its closed now. Force to OFFLINE and this
            // will get it reassigned if appropriate
            forceOffline(regionInfo, data);
        } else {
            // Just insert region into RIT.
            // If this never updates the timeout will trigger new assignment
            regionsInTransition.put(encodedRegionName, new RegionState(
                    regionInfo, RegionState.State.CLOSING,
                    data.getStamp(), data.getOrigin()));
        }
        failoverProcessedRegions.put(encodedRegionName, regionInfo);
    }

    private void processClosed(HRegionInfo regionInfo, RegionTransitionData data) {
        // Implementation for processing closed regions
        // Region is closed, insert into RIT and handle it
        addToRITandCallClose(regionInfo, RegionState.State.CLOSED, data);
        failoverProcessedRegions.put(encodedRegionName, regionInfo);
    }

    private void processOffline(HRegionInfo regionInfo, RegionTransitionData data) {
        // Implementation for processing offline regions
        // If zk node of the region was updated by a live server skip this
        // region and just add it into RIT.
        if (isOnDeadServer(regionInfo, deadServers) &&
                (data.getOrigin() == null ||
                        !serverManager.isServerOnline(data.getOrigin()))) {
            // Region is offline, insert into RIT and handle it like a closed
            addToRITandCallClose(regionInfo, RegionState.State.OFFLINE, data);
        } else if (data.getOrigin() != null &&
                !serverManager.isServerOnline(data.getOrigin())) {
            // to handle cases where offline node is created but sendRegionOpen
            // RPC is not yet sent
            addToRITandCallClose(regionInfo, RegionState.State.OFFLINE, data);
        } else {
            regionsInTransition.put(encodedRegionName, new RegionState(
                    regionInfo, RegionState.State.PENDING_OPEN, data.getStamp(), data
                    .getOrigin()));
        }
        failoverProcessedRegions.put(encodedRegionName, regionInfo);
    }

    private void processOpening(HRegionInfo regionInfo, RegionTransitionData data) {
        // Implementation for processing opening regions
        // TODO: Could check if it was on deadServers.  If it was, then we could
        // do what happens in TimeoutMonitor when it sees this condition.

        // Just insert region into RIT
        // If this never updates the timeout will trigger new assignment
        if (regionInfo.isMetaTable()) {
            regionsInTransition.put(encodedRegionName, new RegionState(
                    regionInfo, RegionState.State.OPENING, data.getStamp(), data
                    .getOrigin()));
            // If ROOT or .META. table is waiting for timeout monitor to assign
            // it may take lot of time when the assignment.timeout.period is
            // the default value which may be very long.  We will not be able
            // to serve any request during this time.
            // So we will assign the ROOT and .META. region immediately.
            processOpeningState(regionInfo);
    }

    private void processOpened(HRegionInfo regionInfo, RegionTransitionData data, int expectedVersion) {
        // Implementation for processing opened regions
            regionsInTransition.put(encodedRegionName, new RegionState(regionInfo,
                    RegionState.State.OPENING, data.getStamp(), data.getOrigin()));
            failoverProcessedRegions.put(encodedRegionName, regionInfo);
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
