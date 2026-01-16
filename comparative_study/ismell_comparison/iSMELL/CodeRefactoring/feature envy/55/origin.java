void processRegionsInTransition(final RegionTransitionData data,
final HRegionInfo regionInfo,
final Map<ServerName, List<Pair<HRegionInfo, Result>>> deadServers,
        int expectedVersion)
        throws KeeperException {
        String encodedRegionName = regionInfo.getEncodedName();
        LOG.info("Processing region " + regionInfo.getRegionNameAsString() +
        " in state " + data.getEventType());
        List<HRegionInfo> hris = this.enablingTables.get(regionInfo.getTableNameAsString());
        if (hris != null && !hris.isEmpty()) {
        hris.remove(regionInfo);
        }
synchronized (regionsInTransition) {
        RegionState regionState = regionsInTransition.get(encodedRegionName);
        if (regionState != null ||
        failoverProcessedRegions.containsKey(encodedRegionName)) {
        // Just return
        return;
        }
        switch (data.getEventType()) {
        case M_ZK_REGION_CLOSING:
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
        break;

        case RS_ZK_REGION_CLOSED:
        case RS_ZK_REGION_FAILED_OPEN:
        // Region is closed, insert into RIT and handle it
        addToRITandCallClose(regionInfo, RegionState.State.CLOSED, data);
        failoverProcessedRegions.put(encodedRegionName, regionInfo);
        break;

        case M_ZK_REGION_OFFLINE:
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
        break;

        case RS_ZK_REGION_OPENING:
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
        break;
        }
        regionsInTransition.put(encodedRegionName, new RegionState(regionInfo,
        RegionState.State.OPENING, data.getStamp(), data.getOrigin()));
        failoverProcessedRegions.put(encodedRegionName, regionInfo);
        break;

        case RS_ZK_REGION_OPENED:
        // Region is opened, insert into RIT and handle it
        regionsInTransition.put(encodedRegionName, new RegionState(
        regionInfo, RegionState.State.OPEN,
        data.getStamp(), data.getOrigin()));
        ServerName sn = data.getOrigin() == null? null: data.getOrigin();
        // sn could be null if this server is no longer online.  If
        // that is the case, just let this RIT timeout; it'll be assigned
        // to new server then.
        if (sn == null) {
        LOG.warn("Region in transition " + regionInfo.getEncodedName() +
        " references a null server; letting RIT timeout so will be " +
        "assigned elsewhere");
        } else if (!serverManager.isServerOnline(sn)
        && (isOnDeadServer(regionInfo, deadServers)
        || regionInfo.isMetaRegion() || regionInfo.isRootRegion())) {
        forceOffline(regionInfo, data);
        } else {
        new OpenedRegionHandler(master, this, regionInfo, sn, expectedVersion)
        .process();
        }
        failoverProcessedRegions.put(encodedRegionName, regionInfo);
        break;
        }
        }
        }
