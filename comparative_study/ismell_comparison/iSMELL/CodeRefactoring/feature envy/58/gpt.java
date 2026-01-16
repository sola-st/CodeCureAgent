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
        }
    }