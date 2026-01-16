//TreeToolController.java
public class TreeToolController {
    private SeedService seedService;
    private AuthorityManager authorityManager;
    private MessageSource messageSource;

    public TreeToolController(SeedService seedService, AuthorityManager authorityManager, MessageSource messageSource) {
        this.seedService = seedService;
        this.authorityManager = authorityManager;
        this.messageSource = messageSource;
    }

    public ModelAndView processOther(TabbedController tc, Tab currentTab, HttpServletRequest req, HttpServletResponse res, Object comm, BindException errors) {
        SeedsCommand command = (SeedsCommand) comm;
        TargetEditorContext ctx = getEditorContext(req);
        seedService = new SeedService(ctx, authorityManager, businessObjectFactory, targetManager, messageSource);

        TabbedController.TabbedModelAndView tmav = tc.preProcessNextTab(currentTab, req, res, comm, errors);
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
            case "ACTION_REMOVE_SELECTED":
                seedService.removeSelectedSeeds(req);
                break;
            case "TOGGLE_PRIMARY":
                seedService.togglePrimarySeed(command.getSelectedSeed());
                break;
            case "UNLINK":
                seedService.unlinkSeed(command.getSelectedSeed(), command.getSelectedPermission());
                break;
            case "ACTION_UNLINK_SELECTED":
                seedService.unlinkSelectedSeeds(req);
                break;
            case "LINK_NEW":
                return seedService.processLinkNew(command, errors, ctx, tc, currentTab, req, res);
            case "ACTION_LINK_SELECTED":
                return seedService.processLinkSelected(command, errors, ctx, tc, currentTab, req, res);
            case "LINK_NEW_CONFIRM":
                return seedService.processLinkNewConfirm(command, errors, ctx, tc, currentTab, req, res);
            case "START_IMPORT":
                return getImportSeedsTabModel(tc, ctx);
            case "DO_IMPORT":
                return processDoImport(command, errors, ctx, tc, currentTab, req, res);
            case "LINK_NEW_CANCEL":
                break;
            default:
                break;
        }

        return tmav;
    }

    private ModelAndView processDoImport(SeedsCommand command, BindException errors, TargetEditorContext ctx, TabbedController tc, Tab currentTab, HttpServletRequest req, HttpServletResponse res) {
        BufferedReader reader = null;
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) req;
        CommonsMultipartFile file = (CommonsMultipartFile) multipartRequest.getFile("seedsFile");
        if (command.getSeedsFile().length != 0 && file.getOriginalFilename() != null && !"".equals(file.getOriginalFilename().trim())) {
            if (!file.getOriginalFilename().endsWith(".txt") && !"text/plain".equals(file.getContentType())) {
                errors.reject("target.seeds.import.filetype");
                ModelAndView tmav = getImportSeedsTabModel(tc, ctx);
                tmav.addObject("org.springframework.validation.BindException.command", errors);
                return tmav;
            }

            try {
                reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(command.getSeedsFile())));
                List<Seed> validSeeds = new LinkedList<>();
                boolean success = true;
                int lineNumber = 1;
                String line = reader.readLine();
                SeedsCommand importCommand = new SeedsCommand();

                while (success && line != null) {
                    if (!line.startsWith("#") && !"".equals(line.trim())) {
                        Seed seed = businessObjectFactory.newSeed(ctx.getTarget());
                        importCommand.setSeed(line);
                        seed.setSeed(importCommand.getSeed());
                        if (UrlUtils.isUrl(seed.getSeed())) {
                            mapSeed(ctx, seed, command.getPermissionMappingOption());
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
                    for (Seed seed : validSeeds) {
                        ctx.getTarget().addSeed(seed);
                    }
                } else {
                    errors.reject("target.seeds.import.badline", new Object[]{lineNumber}, "Bad seed detected on line: " + lineNumber);
                }
            } catch (SeedLinkWrongAgencyException e) {
                errors.reject("target.seeds.link.wrong_agency", "One of the selected seeds cannot be linked because it belongs to another agency.");
            } catch (IOException e) {
                errors.reject("target.seeds.import.ioerror");
                log.error("Failed to import seeds", e);
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    log.debug("Failed to close uploaded seeds file", e);
                }
            }

            if (errors.hasErrors()) {
                ModelAndView tmav = getImportSeedsTabModel(tc, ctx);
                tmav.addObject("org.springframework.validation.BindException.command", errors);
                return tmav;
            }

            ModelAndView tmav = tc.preProcessNextTab(currentTab, req, res, command, errors);
            tmav.getTabStatus().setCurrentTab(currentTab);
            return tmav;
        }

        errors.reject("target.seeds.import.nofile");
        ModelAndView tmav = getImportSeedsTabModel(tc, ctx);
        tmav.addObject("org.springframework.validation.BindException.command", errors);
        return tmav;
    }
}
