public class SeedService {
    private TargetEditorContext context;
    private AuthorityManager authorityManager;

    public SeedService(TargetEditorContext context, AuthorityManager authorityManager) {
        this.context = context;
        this.authorityManager = authorityManager;
    }

    public boolean addSeed(SeedsCommand command, BindException errors) {
        if (!authorityManager.hasAtLeastOnePrivilege(context.getTarget(), new String[]{"MODIFY_TARGET", "CREATE_TARGET"})) {
            return false;
        }

        Seed seed = businessObjectFactory.newSeed(context.getTarget());
        seed.setSeed(command.getSeed().trim());
        seed.setPrimary(context.getTarget().getSeeds().size() == 0);
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

    // Additional methods for toggle, unlink, import, etc.
}
    public ModelAndView processOther(TabbedController tc, Tab currentTab, HttpServletRequest req, HttpServletResponse res, Object comm, BindException errors) {
        SeedsCommand command = (SeedsCommand)comm;
        TargetEditorContext ctx = this.getEditorContext(req);
        SeedService seedService = new SeedService(ctx, authorityManager);

        TabbedModelAndView tmav = tc.preProcessNextTab(currentTab, req, res, comm, errors);
        tmav.addObject("command", command);
        tmav.getTabStatus().setCurrentTab(currentTab);

        switch (command.getAction()) {
            case "ADD_SEED":
                boolean addedExclusions = seedService.addSeed(command, errors);
                if (addedExclusions) {
                    tmav.addObject("page_message", messageSource.getMessage("target.linkseeds.exclusions", null, Locale.getDefault()));
                }
                break;
            case "REMOVE_SEED":
                seedService.removeSeed(command.getSelectedSeed());
                break;
            // Handle other actions similarly...
            default:
                break;
        }

        return tmav;
    }
