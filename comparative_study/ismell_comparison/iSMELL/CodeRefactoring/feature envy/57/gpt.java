public class InTrayCommandManager {
    private int pageSize;
    private InTrayCommand intrayCmd;

    public InTrayCommandManager(InTrayCommand intrayCmd, int pageSize) {
        this.intrayCmd = intrayCmd;
        this.pageSize = pageSize;
    }

    public ModelAndView processAction(BindException aErrors) {
        ModelAndView mav = null;

        if (InTrayCommand.ACTION_DELETE_NOTIFICATION.equals(intrayCmd.getAction())) {
            mav = deleteNotification();
        } else if (InTrayCommand.ACTION_VIEW_NOTIFICATION.equals(intrayCmd.getAction())) {
            mav = viewNotification();
        } else if (InTrayCommand.ACTION_DELETE_TASK.equals(intrayCmd.getAction())) {
            mav = deleteTask(aErrors);
        } else if (InTrayCommand.ACTION_VIEW_TASK.equals(intrayCmd.getAction())) {
            mav = viewTask();
        } else if (InTrayCommand.ACTION_CLAIM_TASK.equals(intrayCmd.getAction())) {
            mav = claimTask();
        } else if (InTrayCommand.ACTION_UNCLAIM_TASK.equals(intrayCmd.getAction())) {
            mav = unclaimTask();
        } else if (InTrayCommand.ACTION_NEXT.equals(intrayCmd.getAction()) || InTrayCommand.ACTION_PREVIOUS.equals(intrayCmd.getAction())) {
            mav = defaultView();
        } else if (InTrayCommand.ACTION_DELETE_ALL_NOTIFICATIONS.equals(intrayCmd.getAction())) {
            mav = deleteAllNotifications();
        }

        return mav;
    }

    // Additional methods to handle each action (deleteNotification, viewNotification, etc.)
}

    protected ModelAndView processFormSubmission(HttpServletRequest aReq, HttpServletResponse aRes, Object aCmd, BindException aErrors) throws Exception {
        InTrayCommand intrayCmd = (InTrayCommand) aCmd;
        ModelAndView mav = null;

        // get value of page size cookie
        String currentPageSize = CookieUtils.getPageSize(aReq);

        int taskPage = intrayCmd.getTaskPage();
        int notificationPage = intrayCmd.getNotificationPage();

        if(intrayCmd.getSelectedPageSize()!=null) {
            if ( !intrayCmd.getSelectedPageSize().equals(currentPageSize) ) {
                // user has selected a new page size, so reset to first page..
                currentPageSize = intrayCmd.getSelectedPageSize();
                CookieUtils.setPageSize(aRes, currentPageSize);
                taskPage = 0;
                notificationPage = 0;
            }
        }

        if (intrayCmd.getAction() != null) {
            int pageSize = Integer.parseInt(currentPageSize);
            InTrayCommandManager manager = new InTrayCommandManager(intrayCmd, pageSize);
            mav = manager.processAction(aErrors);
        } else {
            //invalid action command so redirect to the view intray screen
            log.warn("A form was posted to the InTrayController without a valid action attribute, redirecting to the showForm flow.");
            return showForm(aReq,aRes,a