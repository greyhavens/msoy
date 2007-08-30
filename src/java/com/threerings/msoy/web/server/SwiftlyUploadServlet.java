//
// $Id$

package com.threerings.msoy.web.server;

import static com.threerings.msoy.Log.log;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.StringUtil;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

public class SwiftlyUploadServlet extends AbstractUploadServlet
{
    @Override // from AbstractUploadServlet
    protected void handleFileItems (FileItem item, FileItem[] allItems, int uploadLength,
                                    HttpServletRequest req, HttpServletResponse rsp)
        throws IOException, FileUploadException, AccessDeniedException
    {
        // wrap the FileItem in an UploadFile for publishing
        final UploadFile uploadFile = new FileItemUploadFile(item);

        // attempt to extract the projectId and auth token from the field name
        String field = item.getFieldName();
        if (field == null) {
            throw new FileUploadException("Failed to extract form field from the upload request.");
        }

        // break the form field into the authentication components and the projectId
        String[] split = field.split("::");

        // first try to pull the authentication token out
        String token = split[0];
        if (StringUtil.isBlank(token)) {
            throw new FileUploadException(
                "The authentication token form field from the upload request is blank, aborting.");
        }

        // now pull the integers out
        final int memberId;
        final int projectId;
        try {
            memberId = Integer.parseInt(split[1]);
            projectId = Integer.parseInt(split[2]);

        } catch (NumberFormatException nfe) {
            throw new FileUploadException(
                "Failed to parse integer form field parameters from the upload request. " +
                    "[memberId=" + split[1] + " projectId=" + split[2] + "]");
        }

        // finally, instantiate the WebIdent
        WebIdent ident = new WebIdent(memberId, token);

        // verify this user is logged in and is a collaborator
        checkPermissions(ident, projectId);

        log.info("Swiftly upload: [type=" + item.getContentType() + ", size=" +
            item.getSize() + ", projectId=" + projectId + "].");

        // run a task on the dobject thread that first finds the ProjectRoomManager for this
        // project if it exists, and then commits the file to svn and adds it to the room
        final ServletWaiter<Void> waiter =
            new ServletWaiter<Void>("insertUploadFile[" + projectId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.swiftlyMan.insertUploadFile(projectId, uploadFile, waiter);
            }
        });

        // block the servlet waiting for the dobject thread to complete the upload
        try {
            waiter.waitForResult();
        } catch (ServiceException se) {
            throw new FileUploadException(
                "Inserting the upload file into the room manager failed: " + se);
        }
    }

    @Override // from AbstractUploadServlet
    protected int getMaxUploadSize ()
    {
        return UPLOAD_MAX_SIZE;
    }

    /**
     * Verify the supplied member can perform the requested upload.
     */
    protected void checkPermissions (WebIdent ident, int projectId)
        throws AccessDeniedException, FileUploadException
    {
        try {
            MemberRecord record = MsoyServiceServlet.requireAuthedUser(ident);
            if (!MsoyServer.swiftlyRepo.isCollaborator(projectId, record.memberId)) {
                throw new AccessDeniedException("Access denied. Not a collaborator: [memberId=" +
                    record.memberId + ", projectId=" + projectId + "]");
            }

        } catch (ServiceException e) {
            throw new AccessDeniedException("Access denied. Member not logged in: [memberId=" +
                ident.memberId + ", projectId=" + projectId + "]");

        } catch (PersistenceException pe) {
            throw new FileUploadException("Failed when trying to check collaborator status");
        }
    }

    /** Restrict all Swiftly file uploads to 4 megabytes. */
    protected static final int UPLOAD_MAX_SIZE = 4 * MEGABYTE;
}
