public class SeedService {
    private TargetEditorContext context;
    private AuthorityManager authorityManager;
    private BusinessObjectFactory businessObjectFactory;
    private TargetManager targetManager;
    private MessageSource messageSource;

    public SeedService(TargetEditorContext context, AuthorityManager authorityManager,
                       BusinessObjectFactory businessObjectFactory, TargetManager targetManager,
                       MessageSource messageSource) {
        this.context = context;
        this.authorityManager = authorityManager;
        this.businessObjectFactory = businessObjectFactory;
        this.targetManager = targetManager;
        this.messageSource = messageSource;
    }

    public boolean addSeed(SeedsCommand command, BindException errors) {
        if (!authorityManager.hasAtLeastOnePrivilege(context.getTarget(), new String[]{"MODIFY_TARGET", "CREATE_TARGET"})) {
            return false;
        }

        Seed seed = businessObjectFactory.newSeed(context.getTarget());
        seed.setSeed(command.getSeed().trim());
        seed.setPrimary(context.getTarget().getSeeds().isEmpty());
        try {
            boolean addedExclusions = mapSeed(context, seed, command.getPermissionMappingOption());
            context.putObject(seed);
            context.getTarget().addSeed(seed);
            return addedExclusions;
        } catch (SeedLinkWrongAgencyException e) {
            errors.reject("target.seeds.link.wrong_agency", "One of the selected seeds cannot be linked because it belongs to another agency.");
            return false;
        }
    }

    public void removeSeed(long seedId) {
        Seed seed = (Seed) context.getObject(Seed.class, seedId);
        context.getTarget().removeSeed(seed);
    }

    public void removeSelectedSeeds(HttpServletRequest req) {
        Set<Seed> selectedSeeds = getSelectedSeeds(req);
        for (Seed seed : selectedSeeds) {
            context.getTarget().removeSeed(seed);
        }
    }

    public void togglePrimarySeed(long seedId) {
        Seed seed = (Seed) context.getObject(Seed.class, seedId);
        if (targetManager.getAllowMultiplePrimarySeeds()) {
            seed.setPrimary(!seed.isPrimary());
        } else {
            List<Seed> seeds = context.getSortedSeeds();
            for (Seed s : seeds) {
                s.setPrimary(false);
            }
            seed.setPrimary(true);
        }
    }

    public void unlinkSeed(long seedId, long permissionId) {
        Seed seed = (Seed) context.getObject(Seed.class, seedId);
        Permission permission = (Permission) context.getObject(Permission.class, permissionId);
        seed.removePermission(permission);
    }

    public void unlinkSelectedSeeds(HttpServletRequest req) {
        Set<Seed> selectedSeeds = getSelectedSeeds(req);
        for (Seed seed : selectedSeeds) {
            seed.getPermissions().clear();
            seed.getTarget().setDirty(true);
        }
    }

    public ModelAndView processLinkNew(SeedsCommand command, BindException errors, TargetEditorContext ctx, TabbedController tc, Tab currentTab, HttpServletRequest req, HttpServletResponse res) {
        SeedsCommand newCommand = new SeedsCommand();
        Seed selectedSeed = (Seed)ctx.getObject(Seed.class, command.getSelectedSeed());
        newCommand.setSelectedSeed(command.getSelectedSeed());
        newCommand.setSearchType("url");
        newCommand.setUrlSearchCriteria(selectedSeed.getSeed());
        boolean doSearch = validateLinkSearch(command, errors);
        return tc.preProcessNextTab(currentTab, req, res, newCommand, errors);
    }

    public ModelAndView processLinkSelected(SeedsCommand command, BindException errors, TargetEditorContext ctx, TabbedController tc, Tab currentTab, HttpServletRequest req, HttpServletResponse res) {
        Set<Seed> selectedSeeds = getSelectedSeeds(req);
        String seedList = selectedSeeds.stream().map(Seed::getIdentity).collect(Collectors.joining(","));
        if (!selectedSeeds.isEmpty()) {
            SeedsCommand newCommand = new SeedsCommand();
            newCommand.setSelectedSeed(seedList);
            newCommand.setSearchType("site");
            newCommand.setSiteSearchCriteria("");
            ctx.putAllObjects(selectedSeeds);
            return tc.preProcessNextTab(currentTab, req, res, newCommand, errors);
        }
        return tc.preProcessNextTab(currentTab, req, res, command, errors);
    }

    public ModelAndView processLinkNewConfirm(SeedsCommand command, BindException errors, TargetEditorContext ctx, TabbedController tc, Tab currentTab, HttpServletRequest req, HttpServletResponse res) {
        ModelAndView tmav = tc.preProcessNextTab(currentTab, req, res, command, errors);
        String[] seedList = command.getSelectedSeed().split(",");
        for (String seedId : seedList) {
            Seed theSeed = (Seed) ctx.getObject(Seed.class, seedId);
            String[] perms = command.getLinkPermIdentity();
            Set<Permission> toLink = new HashSet<>();
            boolean wrongAgencyPermission = false;

            for (String perm : perms) {
                Permission linkPerm = targetManager.loadPermission(ctx, perm);
                toLink.add(linkPerm);
                if (!linkPerm.getOwningAgency().equals(ctx.getTarget().getOwner().getAgency())) {
                    wrongAgencyPermission = true;
                }
            }

            if (!wrongAgencyPermission) {
                try {
                    boolean addedExclusions = linkSeed(ctx.getTarget(), theSeed, toLink);
                    if (addedExclusions) {
                        tmav.addObject("page_message", messageSource.getMessage("target.linkseeds.exclusions", null, Locale.getDefault()));
                    }
                } catch (SeedLinkWrongAgencyException e) {
                    errors.reject("target.seeds.link.wrong_agency", "One of the selected seeds cannot be linked because it belongs to another agency.");
                    boolean doSearch = validateLinkSearch(command, errors);
                    return tc.preProcessNextTab(currentTab, req, res, command, errors);
                }
            }
        }
        return tmav;
    }

    private Set<Seed> getSelectedSeeds(HttpServletRequest req) {
        Set<Seed> selectedSeeds = new HashSet<>();
        Set<Seed> seeds = context.getTarget().getSeeds();
        for (Seed seed : seeds) {
            if (isSelected(req, seed)) {
                selectedSeeds.add(seed);
            }
        }
        return selectedSeeds;
    }
}