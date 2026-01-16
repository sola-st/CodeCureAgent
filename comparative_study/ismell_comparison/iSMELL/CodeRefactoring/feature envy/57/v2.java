protected ModelAndView processFormSubmission(HttpServletRequest aReq, HttpServletResponse aRes, Object aCmd, BindException aErrors) throws Exception {
    InTrayCommand intrayCmd = (InTrayCommand) aCmd;

    // get value of page size cookie
    String currentPageSize = CookieUtils.getPageSize(aReq);

    updatePageSizeIfChanged(intrayCmd, aRes, currentPageSize);

    if (intrayCmd.getAction() != null) {
        int pageSize = Integer.parseInt(currentPageSize);
        return handleInTrayAction(intrayCmd, pageSize, aErrors);
    } else {
        //invalid action command so redirect to the view intray screen
        log.warn("A form was posted to the InTrayController without a valid action attribute, redirecting to the showForm flow.");
        return showForm(aReq,aRes,aErrors);
    }
}

private void updatePageSizeIfChanged(InTrayCommand intrayCmd, HttpServletResponse aRes, String currentPageSize) {
    if(intrayCmd.getSelectedPageSize()!=null && !intrayCmd.getSelectedPageSize().equals(currentPageSize)) {
        // user has selected a new page size, so reset to first page..
        currentPageSize = intrayCmd.getSelectedPageSize();
        CookieUtils.setPageSize(aRes, currentPageSize);
        intrayCmd.setTaskPage(0);
        intrayCmd.setNotificationPage(0);
    }
}

private ModelAndView handleInTrayAction(InTrayCommand intrayCmd, int pageSize, BindException aErrors) {
    switch (intrayCmd.getAction()) {
        case InTrayCommand.ACTION_DELETE_NOTIFICATION:
            return deleteNotification(intrayCmd, pageSize);
        case InTrayCommand.ACTION_VIEW_NOTIFICATION:
            return viewNotification(intrayCmd, pageSize);
        case InTrayCommand.ACTION_DELETE_TASK:
            return deleteTask(intrayCmd, pageSize, aErrors);
        case InTrayCommand.ACTION_VIEW_TASK:
            return viewTask(intrayCmd, pageSize);
        case InTrayCommand.ACTION_CLAIM_TASK:
            return claimTask(intrayCmd, pageSize);
        case InTrayCommand.ACTION_UNCLAIM_TASK:
            return unclaimTask(intrayCmd, pageSize);
        case InTrayCommand.ACTION_NEXT:
        case InTrayCommand.ACTION_PREVIOUS:
            return defaultView(intrayCmd.getTaskPage(), intrayCmd.getNotificationPage(), pageSize);
        case InTrayCommand.ACTION_DELETE_ALL_NOTIFICATIONS:
            return deleteAllNotifications(intrayCmd, pageSize);
        default:
            return null;
    }
}