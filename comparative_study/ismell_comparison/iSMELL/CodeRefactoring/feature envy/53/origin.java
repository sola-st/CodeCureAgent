public ModelAndView processOther(TabbedController tc, Tab currentTab, HttpServletRequest req, HttpServletResponse res, Object comm, BindException errors) {
        SeedsCommand command = (SeedsCommand)comm;
        TargetEditorContext ctx = this.getEditorContext(req);
        TabbedController.TabbedModelAndView tmav;
        if (this.authorityManager.hasAtLeastOnePrivilege(ctx.getTarget(), new String[]{"MODIFY_TARGET", "CREATE_TARGET"})) {
        Seed seed;
        TabbedController.TabbedModelAndView tmav;
        if (command.isAction("ADD_SEED")) {
        if (errors.hasErrors()) {
        tmav = this.preProcessNextTab(tc, currentTab, req, res, comm, errors);
        tmav.addObject("command", command);
        tmav.getTabStatus().setCurrentTab(currentTab);
        return tmav;
        }

        seed = this.businessObjectFactory.newSeed(ctx.getTarget());
        seed.setSeed(command.getSeed().trim());
        seed.setPrimary(ctx.getTarget().getSeeds().size() == 0);
        long option = command.getPermissionMappingOption();
        boolean addedExclusions = false;

        try {
        addedExclusions = this.mapSeed(ctx, seed, option);
        ctx.putObject(seed);
        ctx.getTarget().addSeed(seed);
        } catch (SeedLinkWrongAgencyException var31) {
        errors.reject("target.seeds.link.wrong_agency", new Object[0], "One of the selected seeds cannot be linked because it belongs to another agency.");
        }

        tmav = this.preProcessNextTab(tc, currentTab, req, res, comm, errors);
        tmav.getTabStatus().setCurrentTab(currentTab);
        if (addedExclusions) {
        tmav.addObject("page_message", this.messageSource.getMessage("target.linkseeds.exclusions", new Object[0], Locale.getDefault()));
        }

        return tmav;
        }

        TabbedController.TabbedModelAndView tmav;
        if (command.isAction("REMOVE_SEED")) {
        seed = (Seed)ctx.getObject(Seed.class, command.getSelectedSeed());
        ctx.getTarget().removeSeed(seed);
        tmav = this.preProcessNextTab(tc, currentTab, req, res, comm, errors);
        tmav.getTabStatus().setCurrentTab(currentTab);
        return tmav;
        }

        TabbedController.TabbedModelAndView tmav;
        HashSet selectedSeeds;
        Set seeds;
        Seed theSeed;
        Iterator it;
        if (command.isAction("ACTION_REMOVE_SELECTED")) {
        seeds = ctx.getTarget().getSeeds();
        selectedSeeds = new HashSet();
        it = seeds.iterator();

        while(it.hasNext()) {
        theSeed = (Seed)it.next();
        if (this.isSelected(req, theSeed)) {
        selectedSeeds.add(theSeed);
        }
        }

        it = selectedSeeds.iterator();

        while(it.hasNext()) {
        theSeed = (Seed)it.next();
        ctx.getTarget().removeSeed(theSeed);
        }

        tmav = this.preProcessNextTab(tc, currentTab, req, res, comm, errors);
        tmav.getTabStatus().setCurrentTab(currentTab);
        return tmav;
        }

        if (command.isAction("TOGGLE_PRIMARY")) {
        seed = (Seed)ctx.getObject(Seed.class, command.getSelectedSeed());
        if (this.targetManager.getAllowMultiplePrimarySeeds()) {
        seed.setPrimary(!seed.isPrimary());
        } else {
        List<Seed> seeds = ctx.getSortedSeeds();
        it = seeds.iterator();

        while(it.hasNext()) {
        theSeed = (Seed)it.next();
        theSeed.setPrimary(false);
        }

        seed.setPrimary(true);
        }

        tmav = this.preProcessNextTab(tc, currentTab, req, res, comm, errors);
        tmav.getTabStatus().setCurrentTab(currentTab);
        return tmav;
        }

        TabbedController.TabbedModelAndView tmav;
        if (command.isAction("UNLINK")) {
        seed = (Seed)ctx.getObject(Seed.class, command.getSelectedSeed());
        Permission permission = (Permission)ctx.getObject(Permission.class, command.getSelectedPermission());
        seed.removePermission(permission);
        tmav = this.preProcessNextTab(tc, currentTab, req, res, comm, errors);
        tmav.getTabStatus().setCurrentTab(currentTab);
        return tmav;
        }

        if (command.isAction("ACTION_UNLINK_SELECTED")) {
        seeds = ctx.getTarget().getSeeds();
        Iterator<Seed> it = seeds.iterator();

        while(it.hasNext()) {
        Seed seed = (Seed)it.next();
        if (this.isSelected(req, seed)) {
        Set<Permission> permissions = seed.getPermissions();
        permissions.clear();
        seed.getTarget().setDirty(true);
        }
        }

        tmav = this.preProcessNextTab(tc, currentTab, req, res, comm, errors);
        tmav.getTabStatus().setCurrentTab(currentTab);
        return tmav;
        }

        SeedsCommand newCommand;
        if (command.isAction("LINK_NEW")) {
        newCommand = new SeedsCommand();
        Seed selectedSeed = (Seed)ctx.getObject(Seed.class, command.getSelectedSeed());
        newCommand.setSelectedSeed(command.getSelectedSeed());
        newCommand.setSearchType("url");
        newCommand.setUrlSearchCriteria(selectedSeed.getSeed());
        boolean doSearch = this.validator.validateLinkSearch(command, errors);
        return this.processLinkSearch(tc, ctx.getTarget(), newCommand, doSearch, errors);
        }

        if (command.isAction("ACTION_LINK_SELECTED")) {
        seeds = ctx.getTarget().getSeeds();
        selectedSeeds = new HashSet();
        String seedList = "";
        Iterator<Seed> it = seeds.iterator();

        while(it.hasNext()) {
        Seed seed = (Seed)it.next();
        if (this.isSelected(req, seed)) {
        selectedSeeds.add(seed);
        if (seedList.isEmpty()) {
        seedList = seed.getIdentity();
        } else {
        seedList = seedList + "," + seed.getIdentity();
        }
        }
        }

        if (selectedSeeds.size() > 0) {
        SeedsCommand newCommand = new SeedsCommand();
        newCommand.setSelectedSeed(seedList);
        newCommand.setSearchType("site");
        newCommand.setSiteSearchCriteria("");
        ctx.putAllObjects(selectedSeeds);
        return this.processLinkSearch(tc, ctx.getTarget(), newCommand, false, errors);
        }

        tmav = this.preProcessNextTab(tc, currentTab, req, res, comm, errors);
        tmav.getTabStatus().setCurrentTab(currentTab);
        return tmav;
        }

        if (command.isAction("LINK_NEW_SEARCH")) {
        if (errors.hasErrors()) {
        return this.processLinkSearch(tc, ctx.getTarget(), command, false, errors);
        }

        newCommand = new SeedsCommand();
        newCommand.setSelectedSeed(command.getSelectedSeed());
        newCommand.setSearchType(command.getSearchType());
        newCommand.setSiteSearchCriteria(command.getSiteSearchCriteria());
        newCommand.setUrlSearchCriteria(command.getUrlSearchCriteria());
        newCommand.setPageNumber(command.getPageNumber());
        return this.processLinkSearch(tc, ctx.getTarget(), newCommand, true, errors);
        }

        if (command.isAction("LINK_NEW_CONFIRM")) {
        if (errors.hasErrors()) {
        boolean doSearch = this.validator.validateLinkSearch(command, errors);
        return this.processLinkSearch(tc, ctx.getTarget(), command, doSearch, errors);
        }

        tmav = this.preProcessNextTab(tc, currentTab, req, res, comm, errors);
        tmav.getTabStatus().setCurrentTab(currentTab);
        String[] seedList = command.getSelectedSeed().split(",");

        for(int s = 0; s < seedList.length; ++s) {
        theSeed = (Seed)ctx.getObject(Seed.class, seedList[s]);
        String[] perms = command.getLinkPermIdentity();
        Set<Permission> toLink = new HashSet();
        boolean wrongAgencyPermission = false;

        for(int i = 0; i < perms.length && !wrongAgencyPermission; ++i) {
        Permission linkPerm = this.targetManager.loadPermission(ctx, perms[i]);
        toLink.add(linkPerm);
        if (!linkPerm.getOwningAgency().equals(ctx.getTarget().getOwner().getAgency())) {
        wrongAgencyPermission = true;
        }
        }

        try {
        boolean addedExclusions = this.linkSeed(ctx.getTarget(), theSeed, (Set)toLink);
        if (addedExclusions) {
        tmav.addObject("page_message", this.messageSource.getMessage("target.linkseeds.exclusions", new Object[0], Locale.getDefault()));
        }
        } catch (SeedLinkWrongAgencyException var32) {
        errors.reject("target.seeds.link.wrong_agency", new Object[0], "One of the selected seeds cannot be linked because it belongs to another agency.");
        boolean doSearch = this.validator.validateLinkSearch(command, errors);
        return this.processLinkSearch(tc, ctx.getTarget(), command, doSearch, errors);
        }
        }

        return tmav;
        }

        if (command.isAction("LINK_NEW_CANCEL")) {
        tmav = this.preProcessNextTab(tc, currentTab, req, res, comm, errors);
        tmav.getTabStatus().setCurrentTab(currentTab);
        return tmav;
        }

        if (command.isAction("START_IMPORT")) {
        return this.getImportSeedsTabModel(tc, ctx);
        }

        if (command.isAction("DO_IMPORT")) {
        BufferedReader reader = null;
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest)req;
        CommonsMultipartFile file = (CommonsMultipartFile)multipartRequest.getFile("seedsFile");
        if (command.getSeedsFile().length != 0 && file.getOriginalFilename() != null && !"".equals(file.getOriginalFilename().trim())) {
        if (!file.getOriginalFilename().endsWith(".txt") && !"text/plain".equals(file.getContentType())) {
        errors.reject("target.seeds.import.filetype");
        tmav = this.getImportSeedsTabModel(tc, ctx);
        tmav.addObject("org.springframework.validation.BindException.command", errors);
        return tmav;
        }

        try {
        reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(command.getSeedsFile())));
        List<Seed> validSeeds = new LinkedList();
        boolean success = true;
        int lineNumber = 1;
        String line = reader.readLine();
        SeedsCommand importCommand = new SeedsCommand();

        Seed seed;
        while(success && line != null) {
        if (!line.startsWith("#") && !"".equals(line.trim())) {
        seed = this.businessObjectFactory.newSeed(ctx.getTarget());
        importCommand.setSeed(line);
        seed.setSeed(importCommand.getSeed());
        if (UrlUtils.isUrl(seed.getSeed())) {
        this.mapSeed(ctx, seed, command.getPermissionMappingOption());
        ctx.putObject(seed);
        validSeeds.add(seed);
        } else {
        success = false;
        }
        }

        if (success) {
        line = reader.readLine();
        ++lineNumber;
        }
        }

        if (success) {
        Iterator var18 = validSeeds.iterator();

        while(var18.hasNext()) {
        seed = (Seed)var18.next();
        ctx.getTarget().addSeed(seed);
        }
        } else {
        errors.reject("target.seeds.import.badline", new Object[]{lineNumber}, "Bad seed detected on line: " + lineNumber);
        }
        } catch (SeedLinkWrongAgencyException var33) {
        errors.reject("target.seeds.link.wrong_agency", new Object[0], "One of the selected seeds cannot be linked because it belongs to another agency.");
        } catch (IOException var34) {
        errors.reject("target.seeds.import.ioerror");
        log.error("Failed to import seeds", var34);
        } finally {
        try {
        reader.close();
        } catch (Exception var30) {
        log.debug("Failed to close uploaded seeds file", var30);
        }

        }

        if (errors.hasErrors()) {
        tmav = this.getImportSeedsTabModel(tc, ctx);
        tmav.addObject("org.springframework.validation.BindException.command", errors);
        return tmav;
        }

        tmav = this.preProcessNextTab(tc, currentTab, req, res, comm, errors);
        tmav.getTabStatus().setCurrentTab(currentTab);
        return tmav;
        }

        errors.reject("target.seeds.import.nofile");
        tmav = this.getImportSeedsTabModel(tc, ctx);
        tmav.addObject("org.springframework.validation.BindException.command", errors);
        return tmav;
        }
        }

        tmav = this.preProcessNextTab(tc, currentTab, req, res, comm, errors);
        tmav.getTabStatus().setCurrentTab(currentTab);
        return tmav;
        }
