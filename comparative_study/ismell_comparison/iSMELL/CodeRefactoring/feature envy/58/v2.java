public class MemberAuthenticator {
    private MemberDAO memberDAO;

    public MemberAuthenticator(MemberDAO memberDAO) {
        this.memberDAO = memberDAO;
    }

    public MemberBean authenticateMember(GenericRequest request, String loginName, String password, boolean isEncodedPassword) throws AuthenticationException, DatabaseException {
        int memberID = MemberCache.getInstance().getMemberIDFromMemberName(loginName);
        MemberBean memberBean = memberDAO.getMember(memberID);

        if (memberBean.getMemberStatus() != MemberBean.MEMBER_STATUS_ENABLE) {
            if (memberID != MVNForumConstant.MEMBER_ID_OF_ADMIN) {
                throw new AuthenticationException(NotLoginException.ACCOUNT_DISABLED);
            }
        }

        boolean enablePortlet = MvnCoreServiceFactory.getMvnCoreService().getEnvironmentService().isPortlet();
        if (!enablePortlet && !memberDAO.getActivateCode(memberID).equals(MemberBean.MEMBER_ACTIVATECODE_ACTIVATED)) {
            if (MVNForumConfig.getRequireActivation() && memberID != MVNForumConstant.MEMBER_ID_OF_ADMIN) {
                throw new AuthenticationException(NotLoginException.NOT_ACTIVATED);
            }
        }

        if (!validatePassword(loginName, password, isEncodedPassword) && (!MVNForumConfig.getEnablePasswordlessAuth() || password.length() > 0)) {
            throw new AuthenticationException(NotLoginException.WRONG_PASSWORD);
        }

        if (request.getRemoteAddr() != null && !request.getRemoteAddr().equals(MVNCoreGlobal.UN_KNOWN_IP)) {
            Timestamp now = DateUtil.getCurrentGMTTimestamp();
            memberDAO.updateLastLogon(memberID, now, request.getRemoteAddr());
        }

        return memberBean;
    }
}

    public OnlineUser getAuthenticatedUser(GenericRequest request,
                                           GenericResponse response,
                                           String loginName, String password,
                                           boolean isEncodedPassword)
            throws AuthenticationException, DatabaseException {

        try {
            MemberAuthenticator authenticator = new MemberAuthenticator(DAOFactory.getMemberDAO());
            MemberBean memberBean = authenticator.authenticateMember(request, loginName, password, isEncodedPassword);

            // Additional code to construct the OnlineUser object based on the authenticated MemberBean
        } catch (ObjectNotFoundException e) {
            throw new AuthenticationException(NotLoginException.WRONG_NAME);//we don't want this line to happen
        } catch (DatabaseException e) {
            MvnCoreServiceFactory.getMvnCoreService().getEnvironmentService().setShouldRun(false, "Assertion in OnlineUserFactoryImpl.");
            log.error("Unexpected error validating user", e);
            throw new AuthenticationException(NotLoginException.NOT_LOGIN);//we don't want this line to happen
        }
    }