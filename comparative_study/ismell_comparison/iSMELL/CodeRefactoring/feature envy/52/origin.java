protected ModelAndView handle(HttpServletRequest req, HttpServletResponse res, Object comm, BindException errors) throws Exception {
        TreeToolCommand command = (TreeToolCommand)comm;
        TargetInstance ti = (TargetInstance)req.getSession().getAttribute("sessionTargetInstance");
        ArrayList removeElements;
        Element aqaFile;
        if (command.getLoadTree() != null) {
        log.info("Generating Tree");
        HarvestResourceNodeTreeBuilder treeBuilder = this.qualityReviewFacade.getHarvestResultTree(command.getLoadTree());
        WCTNodeTree tree = treeBuilder.getTree();
        req.getSession().setAttribute("tree", tree);
        command.setHrOid(command.getLoadTree());
        log.info("Tree complete");
        if (this.autoQAUrl != null && this.autoQAUrl.length() > 0) {
        removeElements = new ArrayList();

        File xmlFile;
        try {
        xmlFile = this.harvestCoordinator.getLogfile(ti, command.getLogFileName());
        } catch (Exception var28) {
        xmlFile = null;
        log.info("Missing AQA report file: " + command.getLogFileName());
        }

        if (xmlFile != null) {
        Document aqaResult = this.readXMLDocument(xmlFile);
        NodeList parentElementsNode = aqaResult.getElementsByTagName("missingElements");
        if (parentElementsNode.getLength() > 0) {
        NodeList elementNodes = ((Element)parentElementsNode.item(0)).getElementsByTagName("element");

        for(int i = 0; i < elementNodes.getLength(); ++i) {
        aqaFile = (Element)elementNodes.item(i);
        if (aqaFile.getAttribute("statuscode").equals("200")) {
        removeElements.add(new AQAElement(this, aqaFile.getAttribute("url"), aqaFile.getAttribute("contentfile"), aqaFile.getAttribute("contentType"), Long.parseLong(aqaFile.getAttribute("contentLength")), (AQAElement)null));
        }
        }
        }

        req.getSession().setAttribute("aqaImports", removeElements);
        }
        }
        }

        WCTNodeTree tree = (WCTNodeTree)req.getSession().getAttribute("tree");
        List<AQAElement> imports = (List)req.getSession().getAttribute("aqaImports");
        ModelAndView mav;
        if (errors.hasErrors()) {
        mav = new ModelAndView(this.getSuccessView());
        mav.addObject("tree", tree);
        mav.addObject("command", command);
        mav.addObject("aqaImports", imports);
        mav.addObject("org.springframework.validation.BindException.command", errors);
        if (this.autoQAUrl != null && this.autoQAUrl.length() > 0) {
        mav.addObject("showAQAOption", 1);
        } else {
        mav.addObject("showAQAOption", 0);
        }

        return mav;
        } else {
        String aqaUrl;
        if (command.isAction("treeAction")) {
        if (command.getToggleId() != null) {
        tree.toggle(command.getToggleId());
        }

        if (command.getMarkForDelete() != null) {
        tree.markForDelete(command.getMarkForDelete(), command.getPropagateDelete());
        }

        if (command.getTargetURL() != null) {
        HarvestResourceNodeTreeBuilder tb = new HarvestResourceNodeTreeBuilder();

        try {
        tb.getParent(new URL(command.getTargetURL()));
        } catch (MalformedURLException var29) {
        errors.reject("tools.errors.invalidtargeturl");
        ModelAndView mav = new ModelAndView(this.getSuccessView());
        mav.addObject("tree", tree);
        mav.addObject("command", command);
        mav.addObject("aqaImports", imports);
        mav.addObject("org.springframework.validation.BindException.command", errors);
        if (this.autoQAUrl != null && this.autoQAUrl.length() > 0) {
        mav.addObject("showAQAOption", 1);
        } else {
        mav.addObject("showAQAOption", 0);
        }

        return mav;
        }

        if (command.getImportType().equals("file")) {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest)req;
        CommonsMultipartFile uploadedFile = (CommonsMultipartFile)multipartRequest.getFile("sourceFile");
        aqaUrl = UUID.randomUUID().toString();
        File xfrFile = new File(this.uploadedFilesDir + aqaUrl);
        StringBuffer buf = new StringBuffer();
        buf.append("HTTP/1.1 200 OK\n");
        buf.append("Content-Type: ");
        buf.append(uploadedFile.getContentType() + "\n");
        buf.append("Content-Length: ");
        buf.append(uploadedFile.getSize() + "\n");
        buf.append("Connection: close\n\n");
        FileOutputStream fos = new FileOutputStream(xfrFile);
        fos.write(buf.toString().getBytes());
        fos.write(uploadedFile.getBytes());
        fos.close();
        tree.insert(command.getTargetURL(), xfrFile.length(), aqaUrl, uploadedFile.getContentType());
        }

        if (command.getImportType().equals("URL")) {
        String tempFileName = UUID.randomUUID().toString();
        File xfrFile = new File(this.uploadedFilesDir + tempFileName);
        FileOutputStream fos = new FileOutputStream(xfrFile);
        String[] outStrings = new String[1];

        label401: {
        ModelAndView var66;
        try {
        fos.write(this.fetchHTTPResponse(command.getSourceURL(), outStrings));
        break label401;
        } catch (HTTPGetException var30) {
        errors.reject("tools.errors.httpgeterror", new Object[]{command.getSourceURL(), var30.getMessage()}, "");
        ModelAndView mav = new ModelAndView(this.getSuccessView());
        mav.addObject("tree", tree);
        mav.addObject("aqaImports", imports);
        mav.addObject("command", command);
        mav.addObject("org.springframework.validation.BindException.command", errors);
        if (this.autoQAUrl != null && this.autoQAUrl.length() > 0) {
        mav.addObject("showAQAOption", 1);
        } else {
        mav.addObject("showAQAOption", 0);
        }

        var66 = mav;
        } finally {
        fos.close();
        }

        return var66;
        }

        tree.insert(command.getTargetURL(), xfrFile.length(), tempFileName, outStrings[0]);
        }
        }

        mav = new ModelAndView(this.getSuccessView());
        mav.addObject("tree", tree);
        mav.addObject("aqaImports", imports);
        mav.addObject("command", command);
        if (this.autoQAUrl != null && this.autoQAUrl.length() > 0) {
        mav.addObject("showAQAOption", 1);
        } else {
        mav.addObject("showAQAOption", 0);
        }

        return mav;
        } else {
        Long instanceOid;
        String url;
        if (command.isAction("view")) {
        HarvestResource resource = (HarvestResource)((Node)tree.getNodeCache().get(command.getSelectedRow())).getSubject();
        instanceOid = resource.getResult().getOid();
        url = resource.getName();
        return this.enableAccessTool && this.harvestResourceUrlMapper != null ? new ModelAndView("redirect:" + this.harvestResourceUrlMapper.generateUrl(resource.getResult(), resource.buildDTO())) : new ModelAndView("redirect:/curator/tools/browse/" + instanceOid + "/" + url);
        } else {
        TargetInstance tinst;
        if (command.isAction("showHopPath")) {
        tinst = (TargetInstance)req.getSession().getAttribute("sessionTargetInstance");
        instanceOid = tinst.getOid();
        url = command.getSelectedUrl();
        return new ModelAndView("redirect:/curator/target/show-hop-path.html?targetInstanceOid=" + instanceOid + "&logFileName=sortedcrawl.log&url=" + url);
        } else {
        Iterator remit;
        if (command.isAction("aqa")) {
        if (command.getAqaImports() != null) {
        removeElements = new ArrayList();
        String[] aqaImportUrls = command.getAqaImports();

        label442:
        for(int i = 0; i < aqaImportUrls.length; ++i) {
        aqaUrl = aqaImportUrls[i];
        Iterator iter = imports.iterator();

        while(true) {
        AQAElement elem;
        do {
        if (!iter.hasNext()) {
        continue label442;
        }

        elem = (AQAElement)iter.next();
        } while(!elem.getUrl().equals(aqaUrl));

        aqaFile = null;

        try {
        File aqaFile = this.harvestCoordinator.getLogfile(ti, elem.getContentFile());
        String tempFileName = UUID.randomUUID().toString();
        File xfrFile = new File(this.uploadedFilesDir + tempFileName);
        StringBuffer buf = new StringBuffer();
        buf.append("HTTP/1.1 200 OK\n");
        buf.append("Content-Type: ");
        buf.append(elem.getContentType() + "\n");
        buf.append("Content-Length: ");
        buf.append(elem.getContentLength() + "\n");
        buf.append("Connection: close\n\n");
        FileOutputStream fos = new FileOutputStream(xfrFile);
        fos.write(buf.toString().getBytes());
        FileInputStream fin = new FileInputStream(aqaFile);
        byte[] bytes = new byte[8192];
        int len = false;

        int len;
        while((len = fin.read(bytes)) >= 0) {
        fos.write(bytes, 0, len);
        }

        fos.close();
        fin.close();
        tree.insert(aqaUrl, xfrFile.length(), tempFileName, elem.getContentType());
        removeElements.add(elem);
        } catch (Exception var32) {
        log.info("Missing AQA import file: " + elem.getContentFile());
        }
        }
        }

        remit = removeElements.iterator();

        while(remit.hasNext()) {
        AQAElement rem = (AQAElement)remit.next();
        imports.remove(rem);
        }
        }

        mav = new ModelAndView(this.getSuccessView());
        mav.addObject("tree", tree);
        mav.addObject("aqaImports", imports);
        mav.addObject("command", command);
        if (this.autoQAUrl != null && this.autoQAUrl.length() > 0) {
        mav.addObject("showAQAOption", 1);
        } else {
        mav.addObject("showAQAOption", 0);
        }

        return mav;
        } else if (command.isAction("save")) {
        List<String> uris = new LinkedList();
        remit = tree.getPrunedNodes().iterator();

        while(remit.hasNext()) {
        WCTNode node = (WCTNode)remit.next();
        if (node.getSubject() != null) {
        uris.add(((HarvestResource)node.getSubject()).getName());
        }
        }

        List<HarvestResourceDTO> hrs = new LinkedList();
        Iterator var48 = tree.getImportedNodes().iterator();

        while(var48.hasNext()) {
        HarvestResourceDTO dto = (HarvestResourceDTO)var48.next();
        hrs.add(dto);
        }

        HarvestResult result = this.qualityReviewFacade.copyAndPrune(command.getHrOid(), uris, hrs, command.getProvenanceNote(), tree.getModificationNotes());
        this.removeTree(req);
        this.removeAQAImports(req);
        return new ModelAndView("redirect:/curator/target/target-instance.html?targetInstanceId=" + result.getTargetInstance().getOid() + "&cmd=edit&init_tab=RESULTS");
        } else if (command.isAction("cancel")) {
        this.removeTree(req);
        this.removeAQAImports(req);
        tinst = (TargetInstance)req.getSession().getAttribute("sessionTargetInstance");
        return new ModelAndView("redirect:/curator/target/quality-review-toc.html?targetInstanceOid=" + tinst.getOid() + "&harvestResultId=" + command.getHrOid());
        } else {
        mav = new ModelAndView(this.getSuccessView());
        mav.addObject("tree", tree);
        mav.addObject("command", command);
        mav.addObject("aqaImports", imports);
        mav.addObject("org.springframework.validation.BindException.command", errors);
        if (this.autoQAUrl != null && this.autoQAUrl.length() > 0) {
        mav.addObject("showAQAOption", 1);
        } else {
        mav.addObject("showAQAOption", 0);
        }

        return mav;
        }
        }
        }
        }
        }
        }
