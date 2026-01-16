package com.mvnforum.user;

import java.io.*;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.*;

import javax.servlet.http.HttpServletResponse;

import net.myvietnam.mvncore.exception.*;
import net.myvietnam.mvncore.filter.DisableHtmlTagFilter;
import net.myvietnam.mvncore.interceptor.InterceptorService;
import net.myvietnam.mvncore.security.SecurityUtil;
import net.myvietnam.mvncore.service.*;
import net.myvietnam.mvncore.util.*;
import net.myvietnam.mvncore.web.GenericRequest;
import net.myvietnam.mvncore.web.GenericResponse;
import net.myvietnam.mvncore.web.fileupload.FileItem;
import net.myvietnam.mvncore.web.fileupload.FileUploadException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mvnforum.*;
import com.mvnforum.auth.*;
import com.mvnforum.categorytree.*;
import com.mvnforum.common.PostChecker;
import com.mvnforum.db.*;
import com.mvnforum.search.attachment.AttachmentIndexer;
import com.mvnforum.search.attachment.AttachmentSearchQuery;
import com.mvnforum.search.post.PostIndexer;
import com.mvnforum.service.*;

public class SearchAttachmentProcessor {
    private OnlineUserManager onlineUserManager;
    private CategoryBuilderService categoryBuilderService;
    private CategoryService categoryService;
    private MVNForumPermission permission;
    private AttachmentSearchQuery query;

    public SearchAttachmentProcessor(OnlineUserManager onlineUserManager, CategoryBuilderService categoryBuilderService,
                                     CategoryService categoryService, MVNForumPermission permission,
                                     AttachmentSearchQuery query) {
        this.onlineUserManager = onlineUserManager;
        this.categoryBuilderService = categoryBuilderService;
        this.categoryService = categoryService;
        this.permission = permission;
        this.query = query;
    }

    public void processSearchAttachments(GenericRequest request, GenericResponse response)
            throws AuthenticationException, DatabaseException, BadInputException, IOException, ObjectNotFoundException {

        OnlineUser onlineUser = onlineUserManager.getOnlineUser(request);
        permission = onlineUser.getPermission();

        permission.ensureCanGetAttachmentInAnyForum();

        MyUtil.saveVNTyperMode(request, response);

        buildCategoryTree(request, response);
        processSearchQuery(request, response);
    }

    private void buildCategoryTree(GenericRequest request, GenericResponse response) {
        CategoryBuilder builder = categoryBuilderService.getCategoryTreeBuilder();
        CategoryTree tree = new CategoryTree(builder);
        CategoryTreeListener listener = categoryService.getManagementCategorySelector(request, response, "searchattachments");
        tree.addCategeoryTreeListener(listener);
        request.setAttribute("Result", tree.build());
    }

    private void processSearchQuery(GenericRequest request, GenericResponse response)
            throws BadInputException, DatabaseException, IOException, ObjectNotFoundException {
        String key  = GenericParamUtil.getParameter(request, "key");
        String attachmentName = GenericParamUtil.getParameter(request, "attachmentname");
        
        if ( (key.length() == 0) && (attachmentName.length() == 0) ) {
            return;
        }

        int forumID = GenericParamUtil.getParameterInt(request, "forum", 0);//negative means category
        int offset  = GenericParamUtil.getParameterUnsignedInt(request, "offset", 0);
        int rows    = GenericParamUtil.getParameterUnsignedInt(request, "rows", 20);
        if (rows == 0) {
            rows = 20;// fix NullPointerException when rows = 0
        }

        // offset should be even when divide with rowsToReturn
        offset = (offset / rows) * rows;

        AttachmentSearchQuery query = new AttachmentSearchQuery();

        if (key.length() > 0) {
            query.setSearchString(key);
        }

        if (attachmentName.length() > 0) {
            query.setSearchFileName(attachmentName);
        }

        int searchDate        = GenericParamUtil.getParameterUnsignedInt(request, "date", AttachmentSearchQuery.SEARCH_ANY_DATE);
        int searchBeforeAfter = GenericParamUtil.getParameterInt(request, "beforeafter", AttachmentSearchQuery.SEARCH_NEWER);

        if ((searchDate != AttachmentSearchQuery.SEARCH_ANY_DATE) && (searchDate < 365 * 10)) { // 10 years
            long deltaTime = DateUtil.DAY * searchDate;

            Timestamp now = DateUtil.getCurrentGMTTimestamp();
            Timestamp from = null;
            Timestamp to = null;

            long currentTime = now.getTime();

            if (searchBeforeAfter == AttachmentSearchQuery.SEARCH_NEWER) {
                from = new Timestamp(currentTime - deltaTime);
            } else {// older
                to = new Timestamp(currentTime - deltaTime);
            }
            query.setFromDate(from);
            query.setToDate(to);
        }

        if (forumID > 0) {
            query.setForumId(forumID);
        } else if (forumID < 0) {
            // choose to search in a category
            query.setForumId(forumID);
        } else {
            // forumID equals to 0, it mean global searching
            // just do nothing, Lucene will search all forums (globally)
        }

        query.searchDocuments(offset, rows, permission);
        int hitCount = query.getHitCount();
        Collection result = query.getAttachmentResult();
        
        // Remove attachments that current user do not have permission (actually the AttachmentSearchQuery already return correct value)
        for (Iterator iter = result.iterator(); iter.hasNext(); ) {
            AttachmentBean attachBean = (AttachmentBean)iter.next();
            int currentForumID = attachBean.getForumID();
            if (permission.canGetAttachment(currentForumID) == false) {
                iter.remove();
            } else if (ForumCache.getInstance().getBean(currentForumID).getForumStatus() == ForumBean.FORUM_STATUS_DISABLED) {
                iter.remove();
            }
        }

        if (offset > hitCount) {
            String localizedMessage = MVNForumResourceBundle.getString(locale, "mvncore.exception.BadInputException.offset_greater_than_total_rows");
            throw new BadInputException(localizedMessage);
            //throw new BadInputException("Cannot search with offset > total posts");
        }

        request.setAttribute("rows", new Integer(rows));
        request.setAttribute("TotalAttachs", new Integer(hitCount));
        request.setAttribute("AttachBeans", result);
    }
}
