public class MemberManager {
    private MemberDAO memberDAO;

    public MemberManager(MemberDAO memberDAO) {
        this.memberDAO = memberDAO;
    }

    public MemberBean getMember(int memberID) throws DatabaseException, ObjectNotFoundException {
        return memberDAO.getMember(memberID);
    }

    public void updateLastLogon(int memberID, Timestamp now, String remoteAddr) throws DatabaseException {
        memberDAO.updateLastLogon(memberID, now, remoteAddr);
    }

    public boolean isMemberActivated(int memberID) throws DatabaseException {
        return memberDAO.getActivateCode(memberID).equals(MemberBean.MEMBER_ACTIVATECODE_ACTIVATED);
    }
}

    public OnlineUser getAuthenticatedUser(GenericRequest request, GenericResponse response, String loginName, String password, boolean isEncodedPassword) throws AuthenticationException, DatabaseException {
        MemberManager memberManager = new MemberManager(DAOFactory.getMemberDAO());
        int memberID = MemberCache.getInstance().getMemberIDFromMemberName(loginName);
        MemberBean memberBean = memberManager.getMember(memberID);

        // Rest of the code

        if ((request.getRemoteAddr() != null) && (request.getRemoteAddr().equals(MVNCoreGlobal.UN_KNOWN_IP) == false)) {
            Timestamp now = DateUtil.getCurrentGMTTimestamp();
            memberManager.updateLastLogon(memberID, now, request.getRemoteAddr());
        }

        // Rest of the code
        try{
            timeZone = memberBean.getMemberTimeZone();
            localeName = memberBean.getMemberLanguage();
            lastLogon = memberBean.getMemberLastLogon();
            postsPerPage = memberBean.getMemberPostsPerPage();
            lastLogonIP = memberBean.getMemberLastIP();
            invisible  = memberBean.isInvisible();
            Timestamp creationDate = memberBean.getMemberCreationDate();
            Timestamp expireDate = memberBean.getMemberPasswordExpireDate();

            // check password is expired or not
            boolean passwordExpired = false;
            if (MVNForumConfig.getMaxPasswordDays() == 0) {
                passwordExpired = false;
            } else {
                if (expireDate == null) {
                    expireDate = creationDate;
                    passwordExpired = true;
                }
                if (expireDate.after(creationDate)) {
                    if (DateUtil.getCurrentGMTTimestamp().after(expireDate)) {
                        passwordExpired = true;
                    }
                }
            }
            // next, get the correct name from database
            // Eg: if in database the MemberName is "Admin", and user enter "admin"
            // We will convert "admin" to "Admin"
            String memberName = memberBean.getMemberName();

            OnlineUserImpl authenticatedUser = new OnlineUserImpl(request, false/*isGuest*/);

            authenticatedUser.setMemberID(memberID);
            authenticatedUser.setPasswordExpired(passwordExpired);
            authenticatedUser.setMemberName(memberName);
            authenticatedUser.setInvisible(invisible);
            authenticatedUser.setTimeZone(timeZone);

            //NOTE: This MUST be the only way to get permission for a member,
            // so we prevent getPermission for one user and set for other user
            MVNForumPermission permission = MVNForumPermissionFactory.getAuthenticatedPermission(memberBean);
            authenticatedUser.setPermission(permission);
            authenticatedUser.setLocaleName(localeName);
            authenticatedUser.setLastLogonTimestamp(lastLogon);
            authenticatedUser.setLastLogonIP(lastLogonIP);
            authenticatedUser.setGender(memberBean.getMemberGender() != 0);
            authenticatedUser.setPostsPerPage(postsPerPage);

            return authenticatedUser;
        } catch (ObjectNotFoundException e) {
            throw new AuthenticationException(NotLoginException.WRONG_NAME);//we don't want this line to happen
        } catch (DatabaseException e) {
            MvnCoreServiceFactory.getMvnCoreService().getEnvironmentService().setShouldRun(false, "Assertion in OnlineUserFactoryImpl.");
            log.error("Unexpected error validating user", e);
            throw new AuthenticationException(NotLoginException.NOT_LOGIN);//we don't want this line to happen
        }

        if (enablePortlet == false) {
            if (!memberManager.isMemberActivated(memberID)) {
                // not activated
                if (MVNForumConfig.getRequireActivation()) {
                    if (memberID != MVNForumConstant.MEMBER_ID_OF_ADMIN) {// Admin don't have to activate to login
                        throw new AuthenticationException(NotLoginException.NOT_ACTIVATED);
                    }
                }
            }

            // Rest of the code
            if (validatePassword(loginName, password, isEncodedPassword) == false) {
                if ((MVNForumConfig.getEnablePasswordlessAuth() == false) || (password.length() > 0)) {
                    throw new AuthenticationException(NotLoginException.WRONG_PASSWORD);
                }
            }
        }


    }